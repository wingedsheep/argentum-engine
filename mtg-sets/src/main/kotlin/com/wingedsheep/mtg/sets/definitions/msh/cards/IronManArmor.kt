package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * "+1/+1 for each artifact you control" — a dynamic base P/T recomputed every projection pass.
 * Declared before the card so its initializer runs first (top-level properties initialize in
 * file order).
 */
private val IronManArmorArtifactCount: DynamicAmount =
    DynamicAmount.Count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact)

/**
 * Iron Man Armor — Marvel Super Heroes #248
 * {3} · Artifact — Equipment · Mythic
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+1 and has flying.
 * {2}: If this Equipment isn't a creature, it becomes a 0/0 Construct Hero artifact creature
 * with flying and "This creature gets +1/+1 for each artifact you control" until end of turn.
 * Equip {2}
 *
 * Implementation notes:
 * - The ETB is the in-set Super Suit / Falcon's Wing Harness idiom: [Effects.AttachEquipment] on
 *   a "target creature you control".
 * - The two grants to the equipped creature are the canonical Equipment statics scoped to
 *   [Filters.EquippedCreature] — [ModifyStats] for +2/+1 and [GrantKeyword] for flying.
 * - The `{2}` animation is guarded by the printed intervening "if this Equipment isn't a
 *   creature" test ([Conditions.SourceMatches] over [GameObjectFilter.Noncreature], read from
 *   projected state so a second activation while already animated does nothing), and the
 *   animation itself is a Layer 7b [BecomeCreatureEffect] on the source. `addTypes = ARTIFACT`
 *   keeps it an artifact (CREATURE is always added, existing types are kept, so it also stays an
 *   Equipment and remains attached).
 * - The granted "gets +1/+1 for each artifact you control" is carried by
 *   [BecomeCreatureEffect.dynamicPower] / [BecomeCreatureEffect.dynamicToughness] rather than a
 *   granted [ModifyStats] static: a dynamic base P/T is recomputed every projection pass, so the
 *   animated Equipment's size tracks the artifact count continuously (and counts itself, so a
 *   lone animated Iron Man Armor is a 1/1). Modelling it as a granted static instead would be
 *   silently dropped — the layer projector doesn't read [ModifyStats] granted at runtime. The
 *   only divergence from the printed card is the layer the bonus lands in (7b base P/T instead
 *   of 7c modification), which is observable only if another effect sets this permanent's base
 *   power/toughness afterwards; +1/+1 counters and other pumps still stack on top as normal.
 */
val IronManArmor = card("Iron Man Armor") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+1 and has flying.\n" +
        "{2}: If this Equipment isn't a creature, it becomes a 0/0 Construct Hero artifact " +
        "creature with flying and \"This creature gets +1/+1 for each artifact you control\" " +
        "until end of turn.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    // When this Equipment enters, attach it to target creature you control.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
        description = "When this Equipment enters, attach it to target creature you control."
    }

    // Equipped creature gets +2/+1...
    staticAbility {
        ability = ModifyStats(2, 1, Filters.EquippedCreature)
    }

    // ...and has flying.
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    // {2}: If this Equipment isn't a creature, it becomes a 0/0 Construct Hero artifact creature
    // with flying and "This creature gets +1/+1 for each artifact you control" until end of turn.
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = ConditionalEffect(
            condition = Conditions.SourceMatches(GameObjectFilter.Noncreature),
            effect = BecomeCreatureEffect(
                target = EffectTarget.Self,
                power = DynamicAmount.Fixed(0),
                toughness = DynamicAmount.Fixed(0),
                keywords = setOf(Keyword.FLYING),
                creatureTypes = setOf("Construct", "Hero"),
                addTypes = setOf("ARTIFACT"),
                duration = Duration.EndOfTurn,
                dynamicPower = IronManArmorArtifactCount,
                dynamicToughness = IronManArmorArtifactCount
            )
        )
        description = "{2}: If this Equipment isn't a creature, it becomes a 0/0 Construct Hero " +
            "artifact creature with flying and \"This creature gets +1/+1 for each artifact you " +
            "control\" until end of turn."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "248"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/361c2f3b-f04e-446b-a683-9195e238daf0.jpg?1783902891"
    }
}
