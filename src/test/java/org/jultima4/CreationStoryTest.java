package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CreationStoryTest {
  private static CreationStory story() throws Exception {
    return CreationStory.load();
  }

  private static void readIntroduction(CreationStory story) {
    for (int i = 0; i < 24; i++) story.advance();
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "TREE",
        "PORTAL",
        "OUTSIDE",
        "INSIDE",
        "ABACUS",
        "HONCOM",
        "VALJUS",
        "SACHONOR",
        "SPIRHUM"
      })
  void load_whenOriginalCreationArtworkIsPackaged_thenDecodeCompleteImage(String name)
      throws Exception {
    EgaPicture picture = EgaPicture.load(name);

    assertThat(List.of(picture.rgba(0, 0) & 255, picture.rgba(319, 199) & 255))
        .containsExactly(255, 255);
  }

  @Test
  void advance_whenReadingIntroduction_thenPreserveAllOriginalPagesAndPictures() throws Exception {
    CreationStory story = story();
    List<String> pages = new ArrayList<>(), pictures = new ArrayList<>();
    for (int i = 0; i < 24; i++) {
      pages.add(story.text());
      pictures.add(story.picture());
      story.advance();
    }

    assertIntroduction(story, pages, pictures);
  }

  private static void assertIntroduction(
      CreationStory story, List<String> pages, List<String> pictures) {
    assertThat(story.phase()).isEqualTo(CreationStory.Phase.QUESTIONS);
    assertThat(pages).hasSize(24).allSatisfy(page -> assertThat(page).isNotBlank());
    assertThat(pages.getFirst()).startsWith("The day is warm");
    assertThat(pages.get(7)).contains("Ankh, the sacred symbol of life");
    assertThat(pages.get(17)).contains("Enter in peace and find your");
    assertThat(pages.get(22)).contains("We have been waiting such a long");
    assertThat(pages.getLast()).contains("eight\nunusual cards", "Let us begin the\ncasting.");
    assertThat(pictures)
        .containsExactly(
            "TREE", "TREE", "TREE", "TREE", "TREE", "TREE", "PORTAL", "TREE", "TREE", "TREE",
            "TREE", "TREE", "TREE", "TREE", "TREE", "OUTSIDE", "OUTSIDE", "OUTSIDE", "INSIDE",
            "INSIDE", "WAGON", "GYPSY", "ABACUS", "ABACUS");
  }

  static IntStream pairIndices() {
    return IntStream.range(0, 28);
  }

  @ParameterizedTest
  @MethodSource("pairIndices")
  void question_whenAnyVirtuePairIsShown_thenMatchOriginalDilemmaAndAnswerOrder(int pair)
      throws Exception {
    byte[] executable = DosGameData.read("TITLE.EXE");
    String[] expected =
        new String(executable, 17444, 23487 - 17444, java.nio.charset.StandardCharsets.US_ASCII)
            .split("\u0000");
    int index = 0;
    Virtue first = null, second = null;
    for (Virtue a : Virtue.values())
      for (Virtue b : Virtue.values()) {
        if (a.ordinal() < b.ordinal() && index++ == pair) {
          first = a;
          second = b;
        }
      }
    CharacterCreation creation = matchingPair(first, second);
    CreationStory story = story();
    readIntroduction(story);

    CreationStory.Question actual = story.question(creation);

    assertQuestion(actual, creation, first, second, expected[pair]);
  }

  private static CharacterCreation matchingPair(Virtue first, Virtue second) {
    for (int seed = 0; seed < 10000; seed++) {
      CharacterCreation creation = new CharacterCreation(new Random(seed));
      if ((creation.option(0) == first && creation.option(1) == second)
          || (creation.option(1) == first && creation.option(0) == second)) return creation;
    }
    throw new IllegalStateException("No seed found for pair");
  }

  private static void assertQuestion(
      CreationStory.Question question,
      CharacterCreation creation,
      Virtue first,
      Virtue second,
      String expected) {
    assertThat(question.first()).isEqualTo(first);
    assertThat(question.second()).isEqualTo(second);
    assertThat(question.text())
        .startsWith("The gypsy places the first two cards")
        .endsWith(expected)
        .contains("A)", "B)");
    assertThat(creation.option(question.option(0))).isEqualTo(first);
    assertThat(creation.option(question.option(1))).isEqualTo(second);
  }

  @Test
  void choiceFinished_whenSevenChoicesComplete_thenRequireBothClosingPagesBeforeReady()
      throws Exception {
    CreationStory story = story();
    readIntroduction(story);
    CharacterScreens screens = new CharacterScreens(new Random(42));
    screens.start();
    List<String> placements = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      CreationStory.Question question = story.question(screens.creation());
      placements.add(question.text().substring(0, question.text().indexOf(" upon")));
      screens.choose(question.option(i % 2));
      story.choiceFinished(screens.creation().complete());
    }
    String first = story.text();
    story.advance();
    String second = story.text();
    CharacterScreens.Stage before = screens.stage();
    story.advance();

    assertConclusion(story, first, second, placements, before);
  }

  private static void assertConclusion(
      CreationStory story,
      String first,
      String second,
      List<String> placements,
      CharacterScreens.Stage stage) {
    assertThat(placements)
        .containsExactly(
            "The gypsy places the first two cards",
            "The gypsy places two more of the cards",
            "The gypsy places two more of the cards",
            "The gypsy places two more of the cards",
            "The gypsy places two more of the cards",
            "The gypsy places two more of the cards",
            "The gypsy places the last two cards");
    assertThat(first).contains("So be it!  Thy path is chosen!");
    assertThat(second)
        .contains("Seek\nthe counsel of thy sovereign.")
        .endsWith("open your eyes to...");
    assertThat(stage).isEqualTo(CharacterScreens.Stage.SELECTION);
    assertThat(story.phase()).isEqualTo(CreationStory.Phase.READY);
  }

  @Test
  void question_whenStillReading_thenRejectChoicePresentation() throws Exception {
    CreationStory story = story();
    assertThatThrownBy(() -> story.question(new CharacterCreation(new Random(0))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void advance_whenChoosingCards_thenRejectSkippingChoices() throws Exception {
    CreationStory story = story();
    readIntroduction(story);
    assertThatThrownBy(story::advance).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void choiceFinished_whenReadingIntroduction_thenRejectSkippingNarrative() throws Exception {
    CreationStory story = story();
    assertThatThrownBy(() -> story.choiceFinished(true)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void start_whenRestartingDuringQuestions_thenReturnToFirstNarrativePage() throws Exception {
    CreationStory story = story();
    readIntroduction(story);
    story.start();
    assertThat(List.of(story.phase().name(), Integer.toString(story.page()), story.text()))
        .containsExactly("INTRODUCTION", "0", CreationStory.load().text());
  }

  @Test
  void decode_whenTruncated_thenRejectIncompleteText() throws Exception {
    byte[] bytes = Arrays.copyOf(DosGameData.read("TITLE.EXE"), 28388);
    assertThatThrownBy(() -> CreationStory.decode(bytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode_whenTerminatorIsMissing_thenRejectCorruptText() throws Exception {
    byte[] bytes = DosGameData.read("TITLE.EXE");
    bytes[28388] = 'x';
    assertThatThrownBy(() -> CreationStory.decode(bytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode_whenNonTextBytesPresent_thenRejectCorruptText() throws Exception {
    byte[] bytes = DosGameData.read("TITLE.EXE");
    bytes[17444] = 1;
    assertThatThrownBy(() -> CreationStory.decode(bytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void option_whenAnswerIsInvalid_thenRejectIt() throws Exception {
    CreationStory story = story();
    readIntroduction(story);
    var question = story.question(new CharacterCreation(new Random(0)));
    assertThatThrownBy(() -> question.option(2)).isInstanceOf(IllegalArgumentException.class);
  }
}
