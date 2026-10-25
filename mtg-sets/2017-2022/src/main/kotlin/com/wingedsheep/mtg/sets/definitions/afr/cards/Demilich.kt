package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Demilich
 * {U}{U}{U}{U}
 * Creature — Skeleton Wizard
 * 4/3
 *
 * This spell costs {U} less to cast for each instant and sorcery spell you've cast this turn.
 * Whenever this creature attacks, exile up to one target instant or sorcery card from your
 * graveyard. Copy it. You may cast the copy.
 * You may cast this card from your graveyard by exiling four instant and/or sorcery cards from
 * your graveyard in addition to paying its other costs.
 *
 * - Self cost reduction: [CostModification.ReduceColoredPerUnit] removes one {U} per instant/sorcery
 *   cast this turn and overflows the rest onto generic (the Sphere of Resistance ruling).
 * - Attack trigger: the Kaervek, the Punisher chain — exile, copy in exile, then a may-cast of the
 *   copy paying its costs; an uncast copy ceases to exist (CR 707.10a).
 * - Graveyard cast: [MayCastSelfFromZones] with an exile-four-instant/sorcery additional cost.
 */
val Demilich = card("Demilich") {
    manaCost = "{U}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Skeleton Wizard"
    power = 4
    toughness = 3
    oracleText = "This spell costs {U} less to cast for each instant and sorcery spell you've cast this turn.\n" +
        "Whenever this creature attacks, exile up to one target instant or sorcery card from your " +
        "graveyard. Copy it. You may cast the copy.\n" +
        "You may cast this card from your graveyard by exiling four instant and/or sorcery cards " +
        "from your graveyard in addition to paying its other costs."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColoredPerUnit(
                symbols = "{U}",
                countSource = CostReductionSource.Dynamic(
                    DynamicAmounts.spellsCastThisTurn(filter = GameObjectFilter.InstantOrSorcery)
                ),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        description = "Whenever this creature attacks, exile up to one target instant or sorcery " +
            "card from your graveyard. Copy it. You may cast the copy."
        val exiledCard = target(
            TargetFilter(GameObjectFilter.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.Pipeline {
            run(Effects.Move(exiledCard, Zone.EXILE))
            val copy = copyCard(exiledCard)
            run(Effects.May(
                Effects.CastFromCollection(copy),
                descriptionOverride = "You may cast the copy.",
            ))
        }
    }

    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.ExileCards(4, GameObjectFilter.InstantOrSorcery),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "53"
        artist = "Daniel Zrom"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a1351ed-b9df-4a6d-aaa1-ec6db673d265.jpg?1783926516"

        ruling("2021-07-23", "While casting Demilich, if you are required to pay an additional cost that includes a generic mana component, Demilich's cost reduction effect can apply to it. For example, if you're required to pay an additional {1} to cast Demilich (perhaps because of Sphere of Resistance), and you've cast five or more instant and/or sorcery spells this turn, you would pay {0} to cast Demilich.")
        ruling("2021-07-23", "If you exile a card as the triggered ability resolves, that card will remain exiled whether you cast the copy or not.")
        ruling("2021-07-23", "Casting the copy happens as part of the resolution of Demilich's triggered ability. If you choose to cast it, you must do so right away. If you choose not to cast it right away, the copy ceases to exist the next time a player would receive priority.")
        ruling("2021-07-23", "You must pay all normal costs for casting the copy.")
        ruling("2021-07-23", "Demilich's last ability doesn't change when you can cast it. In most cases, you can cast Demilich only during your main phase.")
    }
}
