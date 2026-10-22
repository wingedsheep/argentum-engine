package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathbonnet Sprout // Deathbonnet Hulk (Innistrad: Midnight Hunt)
 * {G} Creature — Fungus 1/1 // Creature — Fungus Horror 3/3 (transform)
 *
 * Front — At the beginning of your upkeep, mill a card. Then if there are three or more creature
 *         cards in your graveyard, transform this creature.
 * Back  — At the beginning of your upkeep, you may exile a card from a graveyard. If a creature card
 *         was exiled this way, put a +1/+1 counter on this creature.
 *
 * The front's "then if" is not an intervening if: the graveyard is counted on resolution, after the
 * mill. The back's exile doesn't target (Cemetery Prowler's graveyard pick, but "up to one" for
 * "you may"), and "exiled this way" reads the pile that actually moved ([moveTracked]), as Graveyard
 * Trespasser does.
 */

private const val SPROUT_TEXT = "At the beginning of your upkeep, mill a card. Then if there are three or more " +
    "creature cards in your graveyard, transform this creature."

private const val HULK_TEXT = "At the beginning of your upkeep, you may exile a card from a graveyard. If a " +
    "creature card was exiled this way, put a +1/+1 counter on this creature."

private val DeathbonnetSproutFront = card("Deathbonnet Sprout") {
    manaCost = "{G}"
    typeLine = "Creature — Fungus"
    power = 1
    toughness = 1
    oracleText = "$SPROUT_TEXT\n(To mill a card, put the top card of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Library.mill(1) then Effects.If(
            condition = Conditions.CreatureCardsInGraveyardAtLeast(3),
            then = Effects.Transform(EffectTarget.Self),
        )
        description = SPROUT_TEXT
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2cda10b-7cd5-4cf5-87bd-c3b8c6aa2b47.jpg?1783925585"
    }
}

private val DeathbonnetHulk = card("Deathbonnet Hulk") {
    manaCost = ""
    colorIdentity = "G"
    colorIndicator = "G" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Fungus Horror"
    power = 3
    toughness = 3
    oracleText = HULK_TEXT

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val graveyards = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Any))
            val chosen = chooseUpTo(
                1,
                from = graveyards,
                useTargetingUI = true,
                prompt = "You may exile a card from a graveyard",
                selectedLabel = "Exile",
            )
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            ifNotEmpty(exiled, filter = GameObjectFilter.Creature) {
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
            }
        }
        description = HULK_TEXT
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Filip Burburan"
        flavorText = "They grow up so fast."
        imageUri = "https://cards.scryfall.io/normal/back/a/2/a2cda10b-7cd5-4cf5-87bd-c3b8c6aa2b47.jpg?1783925585"
    }
}

val DeathbonnetSprout: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = DeathbonnetSproutFront,
    backFace = DeathbonnetHulk,
)
