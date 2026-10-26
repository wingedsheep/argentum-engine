package com.wingedsheep.mtg.sets.definitions.onc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ixhel, Scion of Atraxa
 * {1}{W}{B}{G}
 * Legendary Creature — Phyrexian Angel
 * 2/5
 * Flying, vigilance, toxic 2
 * Corrupted — At the beginning of your end step, each opponent who has three or more poison
 * counters exiles the top card of their library face down. You may look at and play those cards
 * for as long as they remain exiled, and you may spend mana as though it were mana of any color to
 * cast those spells.
 *
 * Not an intervening-if: the trigger always fires and the per-opponent poison test runs at
 * resolution. `ForEachPlayer(EachOpponent)` rebinds `Player.You` to each visited opponent, so
 * Feed the Infection's `If(PoisonCountersAtLeast(3))` tests that opponent, and the gather/exile
 * read *their* library (Memory Vessel's per-player impulse). The permission must still go to
 * Ixhel's controller, which the rebind has hidden — `Player.ControllerOfSource` is the reference
 * that survives it. The card is exiled [FaceDownMode.HIDDEN]; a face-down exiled card is visible
 * to whoever holds a may-play permission on it, so [LookAudience.None] keeps the exiling opponent's
 * look overlay out of it (Gonti, Night Minister). `Permanent` expiry is "for as long as they remain
 * exiled", and `withAnyManaColor` relaxes colored pips only — a `{C}` pip still needs colorless.
 */
val IxhelScionOfAtraxa = card("Ixhel, Scion of Atraxa") {
    manaCost = "{1}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Phyrexian Angel"
    power = 2
    toughness = 5
    oracleText = "Flying, vigilance, toxic 2\n" +
        "Corrupted — At the beginning of your end step, each opponent who has three or more poison counters " +
        "exiles the top card of their library face down. You may look at and play those cards for as long as " +
        "they remain exiled, and you may spend mana as though it were mana of any color to cast those spells."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.toxic(2))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.If(
                condition = Conditions.PoisonCountersAtLeast(3),
                then = Effects.Pipeline {
                    val exiledCard = gather(
                        CardSource.TopOfLibrary(count = 1, player = Player.You),
                        lookAudience = LookAudience.None,
                    )
                    exile(exiledCard, Player.You, faceDown = FaceDownMode.HIDDEN)
                    run(
                        Effects.GrantMayPlayFromExile(
                            from = exiledCard,
                            expiry = MayPlayExpiry.Permanent,
                            withAnyManaColor = true,
                            recipient = EffectTarget.PlayerRef(Player.ControllerOfSource),
                        )
                    )
                },
            ),
        )
        description = "Corrupted — At the beginning of your end step, each opponent who has three or more " +
            "poison counters exiles the top card of their library face down. You may look at and play those " +
            "cards for as long as they remain exiled, and you may spend mana as though it were mana of any " +
            "color to cast those spells."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab866ec4-dcb4-47ef-8de1-a369986609c0.jpg?1783918163"
        ruling(
            "2023-02-04",
            "You may look at and cast the exiled cards (and spend mana as though it were mana of any color to do " +
                "so) even if Ixhel leaves the battlefield. If another player gains control of Ixhel, that player " +
                "can't look at or cast the cards, and you still can."
        )
        ruling(
            "2023-02-04",
            "Ixhel doesn't change when you can cast an exiled card. For example, if you exile a creature card " +
                "without flash, you can cast it only during your main phase when the stack is empty."
        )
        ruling("2023-02-04", "Casting an exiled card causes it to leave exile. You can't cast it multiple times.")
    }
}
