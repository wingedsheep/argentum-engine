package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Costs

/**
 * Scrounge for Eternity
 * {2}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Return target creature or Spacecraft card with mana value 5 or less from your graveyard to the battlefield. Then create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 */
val ScroungeForEternity = card("Scrounge for Eternity") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\nReturn target creature or Spacecraft card with mana value 5 or less from your graveyard to the battlefield. Then create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact.or(GameObjectFilter.Creature)))

    spell {
        val graveyardTarget = target("creature or Spacecraft card with mana value 5 or less from your graveyard", TargetObject(
            filter = TargetFilter( 
                GameObjectFilter.Companion.Creature.ownedByYou().manaValueAtMost(5).or(GameObjectFilter.Companion.Permanent.withSubtype("Spacecraft").ownedByYou().manaValueAtMost(5)),
                zone = Zone.GRAVEYARD
            )
        ))

        effect = Effects.Composite(listOf(
            Effects.PutOntoBattlefield(graveyardTarget),
            Effects.CreateLander()
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Konstantin Porubov"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baeff907-017e-4dee-aae1-19dfaab309de.jpg?1752947016"
    }
}
