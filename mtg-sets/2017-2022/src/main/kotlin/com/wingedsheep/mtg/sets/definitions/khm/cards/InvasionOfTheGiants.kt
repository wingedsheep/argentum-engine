package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

private val GiantCard = GameObjectFilter.Any.withSubtype(Subtype.GIANT)

/**
 * Invasion of the Giants — Kaldheim #215
 * {U}{R} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Scry 2.
 * II — Draw a card. Then you may reveal a Giant card from your hand. When you do, this Saga deals
 * 2 damage to target opponent or planeswalker.
 * III — The next Giant spell you cast this turn costs {2} less to cast.
 *
 * Chapter II draws first, then gathers the Giant cards in hand (so the card just drawn counts) and
 * lets the player choose up to one to reveal. Revealing one fires a real CR 603.12 reflexive
 * trigger, whose target is chosen as it goes on the stack — players can respond between the reveal
 * and the damage, per the ruling. Chapter III is the one-shot next-spell cost rider (generic only).
 */
val InvasionOfTheGiants = card("Invasion of the Giants") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Scry 2.\n" +
        "II — Draw a card. Then you may reveal a Giant card from your hand. When you do, this Saga " +
        "deals 2 damage to target opponent or planeswalker.\n" +
        "III — The next Giant spell you cast this turn costs {2} less to cast."

    sagaChapter(1) {
        effect = Effects.Scry(2)
    }

    sagaChapter(2) {
        effect = Effects.Pipeline(
            descriptionOverride = "Draw a card. Then you may reveal a Giant card from your hand. When you do, " +
                "this Saga deals 2 damage to target opponent or planeswalker."
        ) {
            run(Effects.DrawCards(1))
            val giants = gather(
                CardSource.FromZone(zone = Zone.HAND, player = Player.You, filter = GiantCard)
            )
            val revealed = chooseUpTo(1, from = giants, prompt = "You may reveal a Giant card from your hand")
            reveal(revealed, fromZone = Zone.HAND)
            run(
                Effects.If(
                    condition = whenMatches(revealed),
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        descriptionOverride = "this Saga deals 2 damage to target opponent or planeswalker",
                    ) {
                        val opponentOrPlaneswalker = target(Targets.OpponentOrPlaneswalker)
                        effect = Effects.DealDamage(2, opponentOrPlaneswalker)
                    },
                )
            )
        }
    }

    sagaChapter(3) {
        effect = Effects.ReduceNextSpellCost(DynamicAmounts.fixed(2), GiantCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f5075a0-c72a-474c-937c-95dc9205d14f.jpg?1783928196"
        ruling(
            "2021-02-05",
            "The chapter II ability goes on the stack without a target. You choose whether to reveal a Giant card " +
                "from your hand after you have drawn a card. When you do, the reflexive triggered ability triggers. " +
                "This is different than effects that say \"If you do\" in that players can respond after you've " +
                "revealed the Giant card but before damage is dealt."
        )
        ruling(
            "2021-02-05",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, " +
                "add any cost increases, then apply any cost reductions (such as that of the effect of the chapter " +
                "III ability). The mana value of the spell is determined only by its mana cost, no matter what the " +
                "total cost to cast the spell was."
        )
    }
}
