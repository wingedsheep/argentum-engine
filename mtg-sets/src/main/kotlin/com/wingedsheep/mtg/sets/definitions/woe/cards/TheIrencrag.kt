package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.BecomeArtifactEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Irencrag {2}
 * Legendary Artifact
 *
 * {T}: Add {C}.
 * Whenever a legendary creature you control enters, you may have The Irencrag become a legendary
 * Equipment artifact named Everflame, Heroes' Legacy. If you do, it gains equip {3} and
 * "Equipped creature gets +3/+3" and loses all other abilities.
 *
 * The transform is one [BecomeArtifactEffect] with `Duration.Permanent` ("The Irencrag's last
 * ability lasts indefinitely"), stacking floating continuous effects on the artifact itself:
 * Layer 3 renames it (CR 612.8 — LEGENDARY is a supertype and survives, so it stays legendary),
 * Layer 4 sets ARTIFACT + Equipment, Layer 6 wipes its abilities. Equip {3} rides the durable
 * granted-activated-ability record and the +3/+3 rides `grantedStaticAbilities`, so both survive
 * that wipe.
 *
 * Nothing here is once-only: each resolution independently grants another equip {3} and another
 * "+3/+3", so declining is the player's lever — hence the [MayEffect].
 */
val TheIrencrag = card("The Irencrag") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Add {C}.\n" +
        "Whenever a legendary creature you control enters, you may have The Irencrag become a " +
        "legendary Equipment artifact named Everflame, Heroes' Legacy. If you do, it gains equip " +
        "{3} and \"Equipped creature gets +3/+3\" and loses all other abilities."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // Equip {3}, granted by the transform. Mirrors the shape CardBuilder.equipAbility builds.
    val everflameEquip = ActivatedAbility(
        id = AbilityId.generate(),
        cost = AbilityCost.Atom(CostAtom.Mana(ManaCost.parse("{3}"))),
        effect = AttachEquipmentEffect(EffectTarget.BoundVariable("creature you control")),
        targetRequirements = listOf(
            TargetCreature(filter = TargetFilter.CreatureYouControl, id = "creature you control")
        ),
        isEquipAbility = true,
        timing = TimingRule.SorcerySpeed,
        // No descriptionOverride: `isEquipAbility` already renders "Equip {3}", and it renders
        // against the *effective* cost, so an equip discount shows in the menu.
    )

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.legendary().youControl(),
            binding = TriggerBinding.ANY
        )
        effect = MayEffect(
            BecomeArtifactEffect(
                target = EffectTarget.Self,
                cardTypes = setOf("ARTIFACT"),
                subtypes = setOf("Equipment"),
                colors = null,
                loseAllAbilities = true,
                name = "Everflame, Heroes' Legacy",
                grantedAbility = everflameEquip,
                grantedStaticAbilities = listOf(ModifyStats(3, 3)),
                duration = Duration.Permanent
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8051c5ec-54a6-45a8-8945-fb93c5feaa39.jpg?1783915058"
        ruling("2023-09-01", "The Irencrag's last ability lasts indefinitely.")
    }
}
