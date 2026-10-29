package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silversmote Ghoul — Core Set 2021 #122 (canonical printing)
 * {2}{B} · Creature — Zombie Vampire · 3/1
 *
 * At the beginning of your end step, if you gained 3 or more life this turn, return this card from
 * your graveyard to the battlefield tapped.
 * {1}{B}, Sacrifice this creature: Draw a card.
 *
 * The end-step ability functions only from the graveyard (it doesn't trigger if the Ghoul isn't
 * there as the end step begins — ruling) and is an intervening "if" checked against the turn's
 * life-gain tally, which counts life gained while the Ghoul was anywhere and ignores life lost
 * (ruling). The graveyard-guarded move does nothing if the card has left the graveyard by
 * resolution.
 */
val SilversmoteGhoul = card("Silversmote Ghoul") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Vampire"
    power = 3
    toughness = 1
    oracleText = "At the beginning of your end step, if you gained 3 or more life this turn, return " +
        "this card from your graveyard to the battlefield tapped.\n" +
        "{1}{B}, Sacrifice this creature: Draw a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        description = "At the beginning of your end step, if you gained 3 or more life this turn, " +
            "return this card from your graveyard to the battlefield tapped."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "{1}{B}, Sacrifice this creature: Draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Bryan Sola"
        flavorText = "Being trapped between two forms of unlife is a fate worse than mere death."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5444cb-0ecd-4482-a8d8-09332f382dbd.jpg?1783930700"
        ruling(
            "2020-06-23",
            "Silversmote Ghoul's first ability looks at how much life you've gained in the turn, even if " +
                "it wasn't on the battlefield or in your graveyard when you gained life. It doesn't care " +
                "if you also lost life, even if you lost more life than you gained."
        )
        ruling(
            "2020-06-23",
            "If you haven't gained 3 life by the time your end step begins, Silversmote Ghoul's first " +
                "ability won't trigger at all."
        )
        ruling(
            "2020-06-23",
            "If Silversmote Ghoul isn't in your graveyard as your end step begins, its first ability " +
                "won't trigger at all."
        )
    }
}
