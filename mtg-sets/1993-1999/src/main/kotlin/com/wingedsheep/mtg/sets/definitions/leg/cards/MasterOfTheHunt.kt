package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master of the Hunt
 * {2}{G}{G}
 * Creature — Human
 * 2/2
 * {2}{G}{G}: Create a 1/1 green Wolf creature token named Wolves of the Hunt. It has "bands with
 * other creatures named Wolves of the Hunt."
 *
 * The token's "bands with other" (CR 702.22b) is its own ability — a self-scoped [GrantKeyword], the
 * way an inline token carries any quoted static — so any number of Wolves of the Hunt can attack as
 * one band, and two of them blocking the same creature let their controller divide its damage.
 */
val MasterOfTheHunt = card("Master of the Hunt") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    oracleText = "{2}{G}{G}: Create a 1/1 green Wolf creature token named Wolves of the Hunt. It has " +
        "\"bands with other creatures named Wolves of the Hunt.\" (Any creatures named Wolves of the Hunt " +
        "can attack in a band as long as at least one has \"bands with other creatures named Wolves of the " +
        "Hunt.\" Bands are blocked as a group. If at least two creatures named Wolves of the Hunt you " +
        "control, one of which has \"bands with other creatures named Wolves of the Hunt,\" are blocking or " +
        "being blocked by the same creature, you divide that creature's combat damage, not its controller, " +
        "among any of the creatures it's being blocked by or is blocking.)"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            name = WOLVES_OF_THE_HUNT,
            staticAbilities = listOf(
                GrantKeyword(KeywordAbility.bandsWithOtherNamed(WOLVES_OF_THE_HUNT), GroupFilter.source())
            )
        )
        description = "{2}{G}{G}: Create a 1/1 green Wolf creature token named Wolves of the Hunt. " +
            "It has \"bands with other creatures named Wolves of the Hunt.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e6bf56e-2d74-4e4d-a667-885853979377.jpg?1783948046"
    }
}

private const val WOLVES_OF_THE_HUNT = "Wolves of the Hunt"
