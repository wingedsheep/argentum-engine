package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Will, Scion of Peace
 * {1}{W}{U}
 * Legendary Creature — Human Wizard
 * 2/4
 *
 * Vigilance
 * {T}: Spells you cast this turn that are white and/or blue cost {X} less to cast, where X is
 * the amount of life you gained this turn. Activate only as a sorcery.
 *
 * The discount is a turn-scoped, state-held reduction
 * ([Effects.ReduceSpellCostsThisTurn]) rather than a static on Will, for two reasons the rulings
 * spell out: X is fixed when the ability *resolves* (life gained afterwards doesn't raise it), and
 * the effect lasts the turn whether or not Will survives. Only generic mana comes off (CR 601.2f).
 *
 * Mirrored by [RowanScionOfWar], which reads life lost instead of life gained.
 */
val WillScionOfPeace = card("Will, Scion of Peace") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n" +
        "{T}: Spells you cast this turn that are white and/or blue cost {X} less to cast, " +
        "where X is the amount of life you gained this turn. Activate only as a sorcery."

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        effect = Effects.ReduceSpellCostsThisTurn(
            spellFilter = GameObjectFilter.Any.withAnyColor(Color.WHITE, Color.BLUE),
            amount = DynamicAmounts.lifeGainedThisTurn(),
        )
        description = "{T}: Spells you cast this turn that are white and/or blue cost {X} less to cast, " +
            "where X is the amount of life you gained this turn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Ryan Pancoast"
        flavorText = "\"It's easy to cry 'For glory!' and charge those who disagree with you. " +
            "But it takes finesse to turn enemies into allies rather than corpses.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/162088ea-5f99-4244-9427-2fdfb2168fc3.jpg?1783915068"

        ruling(
            "2023-09-01",
            "The value of X is determined only once, at the time Will, Scion of Peace's activated " +
                "ability resolves."
        )
        ruling(
            "2023-09-01",
            "Will's activated ability doesn't change the mana cost or mana value of any spell. It " +
                "changes only the total cost you pay."
        )
        ruling(
            "2023-09-01",
            "Will's activated ability can't reduce the amount of colored mana you pay for a spell. " +
                "It reduces only the generic mana component of that cost."
        )
        ruling(
            "2023-09-01",
            "Will's activated ability counts the total amount of life you gained without taking " +
                "into account any life you lost during that turn. For example, if you gained 3 life " +
                "and lost 3 life earlier in the turn, the cost of white and/or blue spells you cast " +
                "this turn will be reduced by {3}."
        )
    }
}
