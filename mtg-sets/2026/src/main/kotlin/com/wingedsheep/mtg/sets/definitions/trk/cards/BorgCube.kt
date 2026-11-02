package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Borg Cube
 * {5}{W}{B}
 * Artifact — Spacecraft
 * 6/6
 *
 * Affinity for artifact creatures
 * Whenever this Spacecraft enters or attacks, assimilate a creature card exiled with it. If you can't,
 * exile up to one target creature. (To assimilate a card, put it onto the battlefield under your
 * control with a +1/+1 counter. It's a Borg artifact creature and loses all other creature types.)
 * Station
 * 12+ | Artifact creatures you control have flying.
 *
 * - "Affinity for artifact creatures" is the self-cast generic reduction it abbreviates (Urza, Chief
 *   Artificer's shape) — `KeywordAbility.Affinity` takes a single card type.
 * - The target ("up to one target creature") is chosen as the trigger is put on the stack; the
 *   exile half only runs when no creature card is exiled with the Cube, and links what it exiles
 *   so a later trigger can assimilate it.
 * - Like every Spacecraft, it's an artifact creature at its last threshold (12+), where its 6/6 box is.
 */
val BorgCube = card("Borg Cube") {
    manaCost = "{5}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Artifact — Spacecraft"
    power = 6
    toughness = 6
    oracleText = "Affinity for artifact creatures\n" +
        "Whenever this Spacecraft enters or attacks, assimilate a creature card exiled with it. If you " +
        "can't, exile up to one target creature. (To assimilate a card, put it onto the battlefield " +
        "under your control with a +1/+1 counter. It's a Borg artifact creature and loses all other " +
        "creature types.)\n" +
        "Station\n" +
        "12+ | Artifact creatures you control have flying."

    // Affinity for artifact creatures
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(GameObjectFilter.ArtifactCreature)
            )
        )
    }

    fun assimilateOrExile(victim: EffectTarget) = Effects.Pipeline {
        val exiled = gather(CardSource.FromLinkedExile())
        ifNotEmpty(exiled, filter = GameObjectFilter.Creature) {
            val chosen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card exiled with Borg Cube to assimilate"
            )
            move(
                chosen,
                CardDestination.ToZone(Zone.BATTLEFIELD),
                addCounterType = CounterType.PLUS_ONE_PLUS_ONE,
                entersWith = EntersWithCharacteristics.assimilated()
            )
        } orElse {
            run(Effects.ExileLinkedToSource(victim))
        }
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter.Creature, optional = true)
        effect = assimilateOrExile(victim)
        description = "Whenever this Spacecraft enters, assimilate a creature card exiled with it. " +
            "If you can't, exile up to one target creature."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val victim = target(TargetFilter.Creature, optional = true)
        effect = assimilateOrExile(victim)
        description = "Whenever this Spacecraft attacks, assimilate a creature card exiled with it. " +
            "If you can't, exile up to one target creature."
    }

    station()

    val charge12 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 12)

    staticAbility {
        condition = charge12
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge12
        ability = GrantKeyword(Keyword.FLYING, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "227"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e58f7310-3fd5-48d5-b6b7-a5d3e1808bbc.jpg?1792508285"
    }
}
