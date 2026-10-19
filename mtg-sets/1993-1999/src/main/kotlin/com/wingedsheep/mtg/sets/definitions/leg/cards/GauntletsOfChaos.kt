package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gauntlets of Chaos — Legends #278
 * {5} · Artifact
 *
 * {5}, Sacrifice this artifact: Exchange control of target artifact, creature, or land you control
 * and target permanent an opponent controls that shares one of those types with it. If those
 * permanents are exchanged this way, destroy all Auras attached to them.
 *
 * The second target reads the first through dependent target selection:
 * [GameObjectFilter.sharingCardTypeWith] scoped to artifact, creature and land, so a shared
 * enchantment alone doesn't pair them. Both are re-checked at resolution against the types they
 * have then (the 2009 ruling); if either is illegal the exchange doesn't happen, and the
 * [SuccessCriterion.ControlChanged] gate then destroys no Auras.
 */
val GauntletsOfChaos = card("Gauntlets of Chaos") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "{5}, Sacrifice this artifact: Exchange control of target artifact, creature, or land you " +
        "control and target permanent an opponent controls that shares one of those types with it. If " +
        "those permanents are exchanged this way, destroy all Auras attached to them."

    activatedAbility {
        val yours = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Land).youControl())
        )
        val theirs = target(
            TargetFilter(GameObjectFilter.Permanent.opponentControls().sharingCardTypeWith(
                yours, among = setOf(CardType.ARTIFACT, CardType.CREATURE, CardType.LAND)
            ))
        )
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.SacrificeSelf)
        effect = Effects.IfYouDo(
            action = Effects.ExchangeControl(yours, theirs),
            then = Effects.Pipeline {
                val aura = GameObjectFilter.Permanent.withSubtype(Subtype.AURA)
                val onYours = gather(CardSource.AttachedTo(host = yours, filter = aura))
                val onTheirs = gather(CardSource.AttachedTo(host = theirs, filter = aura))
                destroy(onYours)
                destroy(onTheirs)
            },
            successCriterion = SuccessCriterion.ControlChanged,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "278"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f8f11b5-3ba8-4913-b76a-fb469a74864d.jpg?1783948029"
        ruling(
            "2009-10-01",
            "As you activate the ability, the targets you choose must be two artifacts, two creatures, or two " +
                "lands. As the ability resolves, both targets will be legal only if they are two artifacts, two " +
                "creatures, or two lands at that time as well, though they may be a different type than they " +
                "were at the time the ability was activated."
        )
        ruling(
            "2009-10-01",
            "If one of the targets is illegal by the time the ability resolves, the exchange doesn't happen. " +
                "The target that's still legal will remain under its controller's control. Since the exchange " +
                "doesn't happen, no Auras are destroyed. (If both targets are illegal, the ability doesn't resolve.)"
        )
    }
}
