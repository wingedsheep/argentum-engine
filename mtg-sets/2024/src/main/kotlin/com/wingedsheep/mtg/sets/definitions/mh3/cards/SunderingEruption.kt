package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sundering Eruption {2}{R} // Volcanic Fissure
 * Sorcery
 * Destroy target land. Its controller may search their library for a basic land card, put it onto
 * the battlefield tapped, then shuffle. Creatures without flying can't block this turn.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {R}.
 *
 * "Its controller" is [Player.ControllerOf] the destroyed land, resolved from last-known information
 * once the land is gone (and from the battlefield if it survived, e.g. indestructible — the search
 * still happens, per the ruling). The optional fetch runs under [Effects.ForEachPlayer] so the search
 * is that player's own. "Creatures without flying can't block" is a rule-modifying group effect
 * ([Effects.CantBlockGroup] uses a dynamic filter), so it also covers creatures that enter or lose
 * flying later in the turn.
 */
private val SunderingEruptionFront = card("Sundering Eruption") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Its controller may search their library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle. Creatures without flying can't block this turn."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then
            Effects.ForEachPlayer(
                Player.ControllerOf("the destroyed land"),
                listOf(
                    Effects.May(
                        Patterns.Library.searchLibrary(
                            filter = GameObjectFilter.BasicLand,
                            count = 1,
                            destination = SearchDestination.BATTLEFIELD,
                            entersTapped = true,
                        )
                    )
                )
            ) then
            Effects.CantBlockGroup(
                GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Yohann Schepacz"
        flavorText = "\"Idea: Climbing boots also balanced for running.\"\n—Expedition journal entry, very scorched"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50686ac7-346c-43d1-bdaa-28d46a12ad93.jpg?1783911228"
        ruling("2024-06-07", "If the target land isn't destroyed by Sundering Eruption's effect (perhaps because the land has indestructible), its controller still gets to search for a basic land card, and creatures without flying still can't block this turn.")
        ruling("2024-06-07", "The effect of Sundering Eruption prevents all creatures without flying from blocking that turn, including creatures that lose flying after Sundering Eruption resolves and creatures without flying that enter the battlefield later in the turn. Creatures without flying that gain flying later in the turn will be able to block if they have flying when blockers are being declared.")
    }
}

private val VolcanicFissureBack = card("Volcanic Fissure") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {R}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Yohann Schepacz"
        flavorText = "Even the most seasoned adventurers think twice before testing their fortunes on the volcanic teeth of Akoum."
        imageUri = "https://cards.scryfall.io/normal/back/5/0/50686ac7-346c-43d1-bdaa-28d46a12ad93.jpg?1783911228"
    }
}

val SunderingEruption: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SunderingEruptionFront,
    backFace = VolcanicFissureBack,
)
