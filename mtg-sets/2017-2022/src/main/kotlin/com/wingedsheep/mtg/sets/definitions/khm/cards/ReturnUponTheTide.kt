package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Return Upon the Tide — Kaldheim #106
 * {4}{B} · Sorcery
 *
 * Return target creature card from your graveyard to the battlefield. If it's an Elf, create two
 * 1/1 green Elf Warrior creature tokens.
 * Foretell {3}{B}
 *
 * The Elf check runs after the return (the Defy Death shape): an [Effects.If] over
 * [Conditions.TargetMatchesFilter] on the same target handle, which follows the card onto the
 * battlefield, so it reads the permanent's projected subtypes.
 */
val ReturnUponTheTide = card("Return Upon the Tide") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield. If it's an Elf, " +
        "create two 1/1 green Elf Warrior creature tokens.\n" +
        "Foretell {3}{B} (During your turn, you may pay {2} and exile this card from your hand face down. " +
        "Cast it on a later turn for its foretell cost.)"

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.ELF), creature),
                then = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Elf", "Warrior"),
                    count = 2
                ),
            )
    }

    keywordAbility(KeywordAbility.foretell("{3}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "106"
        artist = "Martina Fačková"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/470601b0-9598-487f-bb14-4bfec7ca6d61.jpg?1783928244"
    }
}
