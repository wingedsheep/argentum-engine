package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Down, Down to Goblin-town
 * {2}{B}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Target opponent reveals their hand. You choose a nonland card from it. That player discards
 *     that card.
 * II — Amass Goblins 1.
 * III, IV — Target opponent loses 1 life and you gain 1 life.
 *
 *  - **Chapter I** is the Ego Drain idiom: reveal, gather the revealed hand, `chooseExactly(1)`
 *    filtered to [GameObjectFilter.Nonland], then move with [MoveType.Discard] so discard triggers
 *    (madness, "whenever you discard") actually see it. A hand with no nonland card simply strips
 *    nothing. Note chapters I, III and IV each target independently — the opponent is chosen fresh
 *    when each chapter ability goes on the stack, so a chapter fizzles on its own if its opponent
 *    becomes an illegal target.
 *  - **Chapter II** is [Effects.Amass]`(1, "Goblin")`. Amass is not a target, so it never fizzles;
 *    it mints the 0/0 Goblin Army first if you control no Army.
 *  - **Chapters III and IV** are one ability declared twice. It is *not* a drain: "and you gain
 *    1 life" is a flat gain, so the life gain still happens even if the loss is prevented or the
 *    opponent's life total can't change — hence [Effects.LoseLife] `.then(` [Effects.GainLife] `)`
 *    rather than [Effects.DrainLife], which would tie the gain to the life actually lost.
 */
val DownDownToGoblinTown = card("Down, Down to Goblin-town") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Target opponent reveals their hand. You choose a nonland card from it. That player " +
        "discards that card.\n" +
        "II — Amass Goblins 1. (Put a +1/+1 counter on an Army you control. It's also a Goblin. " +
        "If you don't control an Army, create a 0/0 black Goblin Army creature token first.)\n" +
        "III, IV — Target opponent loses 1 life and you gain 1 life."

    // I — Target opponent reveals their hand. You choose a nonland card from it. That player
    //     discards that card.
    sagaChapter(1) {
        val opponent = target("target opponent to strip a card from", TargetOpponent())
        effect = Effects.Pipeline {
            run(RevealHandEffect(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "opponentHand")
            val chosen = chooseExactly(
                1, from = hand,
                filter = GameObjectFilter.Nonland,
                prompt = "Choose a nonland card to discard",
                alwaysPrompt = true,
                showAllCards = true,
                name = "toDiscard"
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                moveType = MoveType.Discard
            )
        }
    }

    // II — Amass Goblins 1.
    sagaChapter(2) {
        effect = Effects.Amass(1, "Goblin")
    }

    // III, IV — Target opponent loses 1 life and you gain 1 life.
    sagaChapter(3) {
        val opponent = target("target opponent to lose 1 life", TargetOpponent())
        effect = goblinTownDrain(opponent)
    }
    sagaChapter(4) {
        val opponent = target("target opponent to lose 1 life", TargetOpponent())
        effect = goblinTownDrain(opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Rovina Cai"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b72e193c-e030-4936-9b79-c636eff750e1.jpg?1784733900"
    }
}

/** The chapter III / IV ability: the named opponent loses 1 life, then you gain 1 life. */
private fun goblinTownDrain(opponent: EffectTarget): Effect =
    Effects.LoseLife(1, opponent).then(Effects.GainLife(1))
