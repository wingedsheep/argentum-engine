package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oath of Liliana — Eldritch Moon #99 (canonical printing)
 * {2}{B} · Legendary Enchantment
 *
 * When Oath of Liliana enters, each opponent sacrifices a creature of their choice.
 * At the beginning of each end step, if a planeswalker entered the battlefield under your control
 * this turn, create a 2/2 black Zombie creature token.
 *
 * The end-step trigger watches every player's end step and is an intervening "if" over the
 * per-turn entry log, so the planeswalker need not still be on the battlefield, under your control,
 * or a planeswalker, and Oath of Liliana need not have been on the battlefield when it entered
 * (ruling 2016-07-13). The log is one condition, so the trigger fires once no matter how many
 * planeswalkers entered.
 */
val OathOfLiliana = card("Oath of Liliana") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment"
    oracleText = "When Oath of Liliana enters, each opponent sacrifices a creature of their choice.\n" +
        "At the beginning of each end step, if a planeswalker entered the battlefield under your " +
        "control this turn, create a 2/2 black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.PermanentTypeEnteredBattlefieldThisTurn(CardType.PLANESWALKER)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/1/5/15f1f43a-cbdb-4d1b-a254-dadcb2469784.jpg?1783937418"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Wesley Burt"
        flavorText = "\"I'll keep watch. Happy now?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2028f577-e0fb-48dd-89c6-f4a000b0a881.jpg?1783937481"
        ruling(
            "2016-07-13",
            "Oath of Liliana's last ability will trigger even if the planeswalker that entered the battlefield under your control is no longer on the battlefield, no longer under your control, or no longer a planeswalker. This is also true if Oath of Liliana wasn't on the battlefield when the planeswalker entered."
        )
        ruling(
            "2016-07-13",
            "The last ability triggers only once per turn, even if more than one planeswalker entered the battlefield under your control."
        )
        ruling(
            "2016-07-13",
            "Oath of Liliana is legendary, just like the previous Oaths from Oath of the Gatewatch. If you already control an Oath of Liliana, you can still cast a second Oath of Liliana to take advantage of its enters-the-battlefield ability. It will enter the battlefield, causing its first ability to trigger. You will then choose one to keep, and the other will be put into your graveyard."
        )
    }
}
