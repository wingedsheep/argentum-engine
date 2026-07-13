package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Spring-Loaded Sawblades // Bladewheel Chariot (CR 702.167, The Lost Caverns of Ixalan)
 * {1}{W}
 * Artifact // Artifact — Vehicle
 *
 * Front face — Spring-Loaded Sawblades ({1}{W}, Artifact)
 *   Flash
 *   When this artifact enters, it deals 5 damage to target tapped creature an opponent controls.
 *   Craft with artifact {3}{W} ({3}{W}, Exile this artifact, Exile another artifact you control
 *   or an artifact card from your graveyard: Return this card transformed under its owner's
 *   control. Craft only as a sorcery.)
 *
 * Back face — Bladewheel Chariot (Artifact — Vehicle, 5/5)
 *   Tap two other untapped artifacts you control: This Vehicle becomes an artifact creature
 *   until end of turn.
 *   Crew 1
 *
 * Implementation: the front face's ETB is a targeted [Triggers.EntersBattlefield] trigger with a
 * tapped + opponent-controlled creature target ([TargetFilter.TappedCreature.opponentControls])
 * and [Effects.DealDamage] — the damage source defaults to the trigger's source, matching "it
 * deals 5 damage". The `craft(...)` helper wires the exactly-one-artifact material cost
 * (`minCount = 1, maxCount = 1`) with the {3}{W} mana portion (CR 702.167a-b). The back face's
 * self-animate is an activated ability whose cost is [Costs.TapPermanents] with
 * `excludeSelf = true` ("two OTHER untapped artifacts you control" — the Chariot can't tap
 * itself) and whose effect is the same [Effects.BecomeCreature] shape the engine's crew handler
 * uses: CREATURE is added, existing types/subtypes are kept (empty `creatureTypes` sets nothing,
 * so it stays a Vehicle), and base P/T is set to its printed 5/5 until end of turn. Crew 1 is
 * the standard [KeywordAbility.crew] keyword.
 */

private val SpringLoadedSawbladesFront = card("Spring-Loaded Sawblades") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "When this artifact enters, it deals 5 damage to target tapped creature an opponent controls.\n" +
        "Craft with artifact {3}{W} ({3}{W}, Exile this artifact, Exile another artifact you control or an artifact card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    keywords(Keyword.FLASH)

    // ETB: it deals 5 damage to target tapped creature an opponent controls.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target tapped creature an opponent controls",
            TargetCreature(filter = TargetFilter.TappedCreature.opponentControls())
        )
        effect = Effects.DealDamage(5, creature)
    }

    // "Craft with artifact" = exactly one artifact material (CR 702.167a-b).
    craft(
        filter = GameObjectFilter.Artifact,
        cost = "{3}{W}",
        materialDescription = "artifact",
        minCount = 1,
        maxCount = 1
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24417388-f2bb-4783-bdce-264774531838.jpg?1782694581"
    }
}

private val BladewheelChariot = card("Bladewheel Chariot") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Tap two other untapped artifacts you control: This Vehicle becomes an artifact creature until end of turn.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"

    // Tap two other untapped artifacts you control: becomes an artifact creature until EOT.
    // Same animate the crew handler applies: adds CREATURE (it's already an artifact), keeps
    // the Vehicle subtype (empty creatureTypes = no subtype set), base P/T = printed 5/5.
    activatedAbility {
        cost = Costs.TapPermanents(count = 2, filter = GameObjectFilter.Artifact, excludeSelf = true)
        effect = Effects.BecomeCreature(power = 5, toughness = 5)
        description = "Tap two other untapped artifacts you control: This Vehicle becomes an artifact creature until end of turn."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/back/2/4/24417388-f2bb-4783-bdce-264774531838.jpg?1782694581"
    }
}

val SpringLoadedSawblades: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SpringLoadedSawbladesFront,
    backFace = BladewheelChariot
)
