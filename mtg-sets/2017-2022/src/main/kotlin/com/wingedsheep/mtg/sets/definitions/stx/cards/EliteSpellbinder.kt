package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Elite Spellbinder — Strixhaven: School of Mages #17 (canonical printing)
 * {2}{W} · Creature — Human Cleric · 3/1
 *
 * Flying
 * When this creature enters, look at target opponent's hand. You may exile a nonland card from
 * it. For as long as that card remains exiled, its owner may play it. A spell cast this way costs
 * {2} more to cast.
 *
 * The same ETB as Invasion of Gobakhan: a hand look, an optional `chooseUpTo(1)` nonland pick,
 * then [Effects.ExileAndGrantOwnerPlayPermission] for the owner's permanent play permission and
 * the {2} tax. The tax is scoped to opponents of the Spellbinder's controller, which is every
 * caster here — the card came from a target opponent's hand, so its owner is never you.
 */
val EliteSpellbinder = card("Elite Spellbinder") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, look at target opponent's hand. You may exile a nonland card " +
        "from it. For as long as that card remains exiled, its owner may play it. A spell cast " +
        "this way costs {2} more to cast."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LookAtHand(opponent) then
            Effects.Pipeline {
                val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val exiled = chooseUpTo(
                    1,
                    from = opponentHand,
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    prompt = "You may exile a nonland card from target opponent's hand",
                    showAllCards = true,
                    alwaysPrompt = true
                )
                ifNotEmpty(exiled) {
                    run(Effects.ExileAndGrantOwnerPlayPermission(
                        target = exiled.asTarget,
                        opponentCostIncrease = 2
                    ))
                }
            }
        description = "When this creature enters, look at target opponent's hand. You may exile a " +
            "nonland card from it. For as long as that card remains exiled, its owner may play " +
            "it. A spell cast this way costs {2} more to cast."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Ryan Pancoast"
        flavorText = "Paulo Vitor Damo da Rosa, World Champion XXVI"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3a7998-ccac-45ad-a4e9-3a2cb057f63b.jpg?1783927390"
        ruling("2021-04-16", "Playing the exiled card follows all normal timing restrictions.")
        ruling(
            "2021-04-16",
            "If the exiled card is a modal double-faced card and its back face is a land, its " +
                "owner may play it as a land. (Note that if the front face is also a land, you " +
                "couldn't have exiled it to begin with.)"
        )
    }
}
