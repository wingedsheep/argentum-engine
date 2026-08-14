package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.SagaChapterBuilder
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Roll-Roll-Roll-Roll
 * {2}{U}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III, IV — Exile up to one target creature or land you control. If you do, return it to
 * the battlefield under its owner's control at the beginning of the next end step.
 *
 * The same blink ability on all four chapters, so it is declared four times — the engine keys
 * chapter abilities by lore-counter number. Each chapter picks its own target as it goes on the
 * stack, so a permanent blinked by chapter I is back (and re-targetable) long before chapter II.
 *
 * "Up to one target" is [TargetPermanent] with `optional = true`: declining to choose, or the
 * chosen permanent having left the battlefield by resolution, leaves the chapter with nothing to
 * exile — and the delayed end-step return then has nothing to bring back, which is exactly the
 * "If you do" clause. [Patterns.Exile.exileUntilEndStep] returns the card under its owner's
 * control, matching the printed wording, so a permanent you'd stolen goes home rather than
 * returning under your control.
 */
val RollRollRollRoll = card("Roll-Roll-Roll-Roll") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III, IV — Exile up to one target creature or land you control. If you do, return " +
        "it to the battlefield under its owner's control at the beginning of the next end step."

    // I, II, III, IV — Exile up to one target creature or land you control. If you do, return it
    //                  to the battlefield under its owner's control at the beginning of the next
    //                  end step.
    sagaChapter(1) { blinkOneOfYours() }
    sagaChapter(2) { blinkOneOfYours() }
    sagaChapter(3) { blinkOneOfYours() }
    sagaChapter(4) { blinkOneOfYours() }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2e4099e-86bd-461f-87fa-7f7850ae7eec.jpg?1785152396"
    }
}

/** The chapter ability shared by all four chapters: blink up to one of your creatures or lands. */
private fun SagaChapterBuilder.blinkOneOfYours() {
    val permanent = target(
        "up to one target creature or land you control",
        TargetPermanent(
            optional = true,
            filter = TargetFilter.CreatureOrLandPermanent.youControl()
        )
    )
    effect = Patterns.Exile.exileUntilEndStep(permanent)
}
