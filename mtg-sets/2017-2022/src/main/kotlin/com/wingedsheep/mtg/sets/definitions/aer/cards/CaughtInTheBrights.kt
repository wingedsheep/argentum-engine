package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Caught in the Brights — Aether Revolt #10
 * {2}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature can't attack or block.
 * When a Vehicle you control attacks, exile enchanted creature.
 *
 * "Can't attack or block" is the Pacifism idiom — [CantAttack] + [CantBlock] over the attached
 * creature. The trigger is the per-attacker `Triggers.a(filter).attacks()` shape (as on Miriam,
 * Herd Whisperer) scoped to Vehicles you control; "exile enchanted creature" is untargeted, so
 * it reads [EffectTarget.EnchantedCreature] at resolution. If the Aura has left the battlefield by
 * then, that reference falls back to the host the Aura was attached to as it last existed
 * (CR 608.2h), which is exactly the second ruling below.
 */
val CaughtInTheBrights = card("Caught in the Brights") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature can't attack or block.\n" +
        "When a Vehicle you control attacks, exile enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype(Subtype.VEHICLE).youControl()).attacks()
        effect = Effects.Exile(EffectTarget.EnchantedCreature)
        description = "When a Vehicle you control attacks, exile enchanted creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Kieran Yanner"
        flavorText = "While hunting aether, a gremlin may ignore other stimuli, including threats to its own life."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d5bea27-d825-4691-8ae0-c4831574ec53.jpg?1783936782"
        ruling(
            "2017-02-09",
            "After the enchanted creature is exiled, Caught in the Brights is put into its owner's graveyard."
        )
        ruling(
            "2017-02-09",
            "If Caught in the Brights leaves the battlefield in response to its triggered ability, the resolving " +
                "ability will exile the creature Caught in the Brights was enchanting as it left the battlefield."
        )
    }
}
