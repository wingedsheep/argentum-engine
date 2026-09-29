package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of Ikoria // Zilortha, Apex of Ikoria — March of the Machine #190.
 * {X}{G}{G} · Battle — Siege · defense 6 // Legendary Creature — Dinosaur 8/8
 *
 * Front: the ETB search is Vision Quest's hand-rolled "library and/or graveyard" pipeline — one
 * combined pool, capped by the X this battle was cast with ([DynamicAmounts.castX], read off the
 * permanent, since an enters trigger has no X of its own), put onto the battlefield.
 * Back: a battlefield-scoped [AssignCombatDamageAsUnblocked] over non-Human creatures you control;
 * `CombatDamageManager` asks per blocked creature it covers.
 */
private val InvasionOfIkoriaFront = card("Invasion of Ikoria") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Battle — Siege"
    startingDefense = 6
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, search your library and/or graveyard for a non-Human creature " +
        "card with mana value X or less and put it onto the battlefield. If you search your " +
        "library this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromMultipleZones(
                    zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                        .notSubtype(Subtype.HUMAN)
                        .manaValueAtMostDynamic(DynamicAmounts.castX())
                )
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library and/or graveyard for a non-Human creature card " +
                    "with mana value X or less."
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d59c8f2-f6af-40a6-8dfe-8cc45bf231ce.jpg?1783916974"
    }
}

private val ZilorthaApexOfIkoria = card("Zilortha, Apex of Ikoria") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G"
    typeLine = "Legendary Creature — Dinosaur"
    power = 8
    toughness = 8
    oracleText = "Reach\n" +
        "For each non-Human creature you control, you may have that creature assign its combat " +
        "damage as though it weren't blocked."

    keywords(Keyword.REACH)

    staticAbility {
        ability = AssignCombatDamageAsUnblocked(
            filter = GroupFilter(GameObjectFilter.Creature.youControl().notSubtype(Subtype.HUMAN))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/back/5/d/5d59c8f2-f6af-40a6-8dfe-8cc45bf231ce.jpg?1783916974"
    }
}

val InvasionOfIkoria: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfIkoriaFront,
    backFace = ZilorthaApexOfIkoria,
)
