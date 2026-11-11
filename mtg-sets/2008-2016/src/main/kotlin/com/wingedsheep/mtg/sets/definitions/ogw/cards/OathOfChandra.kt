package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oath of Chandra — Oath of the Gatewatch #113 (canonical printing)
 * {1}{R} · Legendary Enchantment
 *
 * When Oath of Chandra enters, it deals 3 damage to target creature an opponent controls.
 * At the beginning of each end step, if a planeswalker entered the battlefield under your control
 * this turn, Oath of Chandra deals 2 damage to each opponent.
 *
 * The end-step trigger watches every player's end step and is an intervening "if" over the
 * per-turn entry log (the same shape as Oath of Liliana), so the planeswalker need not still be on
 * the battlefield, under your control, or a planeswalker, and Oath of Chandra need not have been on
 * the battlefield when it entered (ruling 2016-01-22). It fires once however many entered.
 */
val OathOfChandra = card("Oath of Chandra") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Chandra enters, it deals 3 damage to target creature an opponent controls.\n" +
        "At the beginning of each end step, if a planeswalker entered the battlefield under your control " +
        "this turn, Oath of Chandra deals 2 damage to each opponent."

    triggeredAbility {
        val creature = target(TargetFilter.Creature.opponentControls())
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(3, creature)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.PermanentTypeEnteredBattlefieldThisTurn(CardType.PLANESWALKER)
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Wesley Burt"
        flavorText = "\"If it means that people can live in freedom, yeah, I'll keep watch.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f570b0cb-2f38-422d-89d6-22c85e085328.jpg?1783937906"
        ruling(
            "2016-01-22",
            "Oath of Chandra's last ability will trigger even if the planeswalker that entered the battlefield under your control is no longer on the battlefield, no longer under your control, or no longer a planeswalker. This is also true if Oath of Chandra wasn't on the battlefield when the planeswalker entered."
        )
        ruling(
            "2016-01-22",
            "The last ability triggers only once per turn, even if more than one planeswalker entered the battlefield under your control."
        )
        ruling("2016-01-22", "Each of the Oaths is legendary: if you control more than one of any particular Oath, you choose one to remain on the battlefield and the others are put into your graveyard.")
        ruling("2016-01-22", "The “legend rule” checks the full names of legendary permanents. You can control Oath of Gideon and Oath of Chandra at the same time, for example.")
        ruling("2016-01-22", "Even if you control a particular Oath, you can cast another of that Oath to take advantage of its enters-the-battlefield ability. It will enter the battlefield, causing its first ability to trigger. You will then choose one to keep, as detailed above, and then its enters-the-battlefield ability will resolve.")
    }
}
