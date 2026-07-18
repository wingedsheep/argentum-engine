package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dowsing Device // Geode Grotto (The Lost Caverns of Ixalan)
 * {1}{R}
 * Artifact // Land — Cave
 *
 * Front — Dowsing Device (Artifact, {1}{R})
 *   Whenever this artifact or another artifact you control enters, up to one target creature
 *   you control gets +1/+0 and gains haste until end of turn. Then transform this artifact if
 *   you control four or more artifacts.
 *
 * Back — Geode Grotto (Land — Cave)
 *   {T}: Add {R}.
 *   {2}{R}, {T}: Until end of turn, target creature gains haste and gets +X/+0, where X is the
 *   number of artifacts you control. Activate only as a sorcery.
 *
 * Implementation:
 *  - Front trigger is [Triggers.entersBattlefield]`(Artifact.youControl(), binding = ANY)` — the
 *    ANY binding fires for the device itself and every other artifact you control. The optional
 *    target ("up to one") is a [TargetCreature]`(optional = true)`; the effect pumps +1/+0
 *    ([Effects.ModifyStats]) and grants haste ([Effects.GrantKeyword]) until end of turn, then a
 *    [ConditionalEffect] on [Conditions.YouControlAtLeast]`(4, Artifact)` flips the device. The
 *    transform is gated only on the artifact count, so it still happens when no creature is chosen.
 *  - Back's activated ability grants haste and a dynamic +X/+0 where X counts your artifacts
 *    ([DynamicAmount.AggregateBattlefield]), at sorcery speed ([TimingRule.SorcerySpeed]).
 */

private val DowsingDeviceFront = card("Dowsing Device") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact"
    oracleText = "Whenever this artifact or another artifact you control enters, up to one " +
        "target creature you control gets +1/+0 and gains haste until end of turn. Then " +
        "transform this artifact if you control four or more artifacts."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.ANY,
        )
        val creature = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.Creature.youControl()),
        )
        effect = Effects.Composite(
            Effects.ModifyStats(1, 0, creature),
            Effects.GrantKeyword(Keyword.HASTE, creature),
            ConditionalEffect(
                condition = Conditions.YouControlAtLeast(4, GameObjectFilter.Artifact),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Olena Richards"
        flavorText = "\"A little innovation can lead to a big score.\"\n—Admiral Beckett Brass"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d715e9f-223d-462e-8ce3-eebbaf1cd021.jpg?1782694492"
    }
}

private val GeodeGrotto = card("Geode Grotto") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Land — Cave"
    oracleText = "{T}: Add {R}.\n" +
        "{2}{R}, {T}: Until end of turn, target creature gains haste and gets +X/+0, where X " +
        "is the number of artifacts you control. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.HASTE, creature),
            Effects.ModifyStats(
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact),
                DynamicAmount.Fixed(0),
                creature,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/back/3/d/3d715e9f-223d-462e-8ce3-eebbaf1cd021.jpg?1782694492"
    }
}

val DowsingDevice: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = DowsingDeviceFront,
    backFace = GeodeGrotto,
)
