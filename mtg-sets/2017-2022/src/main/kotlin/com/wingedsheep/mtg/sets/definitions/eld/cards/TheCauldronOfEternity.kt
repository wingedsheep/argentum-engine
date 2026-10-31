package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Cauldron of Eternity — Throne of Eldraine #82
 * {10}{B}{B} · Legendary Artifact
 *
 * The cost reduction is a self-cast [ModifySpellCost] over creature cards in the caster's
 * graveyard (Ghoultree's shape, {2} per card); it only eats generic mana, so {B}{B} always
 * remains, and the mana value stays 12.
 *
 * The death trigger moves the dying card only if it is still in the graveyard when the trigger
 * resolves (Zask, Skittering Swarmlord's shape); a token has ceased to exist and moves nothing.
 */
val TheCauldronOfEternity = card("The Cauldron of Eternity") {
    manaCost = "{10}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact"
    oracleText = "This spell costs {2} less to cast for each creature card in your graveyard.\n" +
        "Whenever a creature you control dies, put it on the bottom of its owner's library.\n" +
        "{2}{B}, {T}, Pay 2 life: Return target creature card from your graveyard to the battlefield. " +
        "Activate only as a sorcery."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Creature,
                    amountPerCard = 2
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.Move(
            EffectTarget.TriggeringEntity,
            Zone.LIBRARY,
            placement = ZonePlacement.Bottom,
            fromZone = Zone.GRAVEYARD,
        )
        description = "Whenever a creature you control dies, put it on the bottom of its owner's library."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Tap, Costs.PayLife(2))
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
        description = "Return target creature card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "82"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb69473f-de99-43a7-b094-429465ae735c.jpg?1783932642"
        ruling(
            "2019-10-04",
            "The cost reduction ability reduces only the generic mana in the relic's cost. " +
                "The colored mana must still be paid."
        )
        ruling(
            "2019-10-04",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, " +
                "add any cost increases, then apply any cost reductions. The mana value of the spell remains " +
                "unchanged, no matter what the total cost to cast it was."
        )
    }
}
