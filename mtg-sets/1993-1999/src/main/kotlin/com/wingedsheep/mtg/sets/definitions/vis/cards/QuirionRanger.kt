package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Quirion Ranger — Visions #117
 * {G} · Creature — Elf Ranger · 1 / 1
 *
 * Return a Forest you control to its owner's hand: Untap target creature. Activate only once each
 * turn.
 *
 * The Forest is the whole cost — there is no mana in it — so [Costs.ReturnToHand] carries the
 * filter. "A Forest" is the land subtype, not the basic land card named Forest, so the filter is
 * `Land.withSubtype(Forest)`: a Savannah or a Dryad Arbor pays it too. The cost atom excludes the
 * source, which costs the Ranger nothing here (it isn't a land).
 *
 * "Activate only once each turn" is [ActivationRestriction.OncePerTurn] — a restriction on the
 * ability, checked at activation, so it is not reset by the Ranger leaving and re-entering the
 * battlefield within a turn.
 */
val QuirionRanger = card("Quirion Ranger") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Ranger"
    power = 1
    toughness = 1
    oracleText = "Return a Forest you control to its owner's hand: Untap target creature. Activate only once each turn."

    activatedAbility {
        cost = Costs.ReturnToHand(GameObjectFilter.Land.withSubtype(Subtype.FOREST))
        val t = target("creature", Targets.Creature)
        effect = Effects.Untap(t)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Tom Kyffin"
        flavorText = "\"Respect the earth, for it will one day be your shield and another day your blanket.\"\n—Liefellen, Quirion exarch"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56efe72c-6d7f-44f6-ac74-01af9305c4b6.jpg?1783946981"
    }
}
