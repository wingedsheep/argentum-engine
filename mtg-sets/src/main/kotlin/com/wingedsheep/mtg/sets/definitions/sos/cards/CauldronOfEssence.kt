package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cauldron of Essence — Secrets of Strixhaven #179
 * {1}{B}{G} · Artifact
 *
 * Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life.
 * {1}{B}{G}, {T}, Sacrifice a creature: Return target creature card from your graveyard
 * to the battlefield. Activate only as a sorcery.
 *
 * The drain trigger is [Triggers.YourCreatureDies] (fires for any creature you control
 * entering your graveyard from the battlefield), composing a 1-life loss for each opponent
 * and a 1-life gain for you. The activated ability mirrors the Doomed Necromancer reanimator
 * shape — mana + tap + sacrifice a creature, returning a targeted creature card from your
 * graveyard — restricted to sorcery speed via [TimingRule.SorcerySpeed].
 */
val CauldronOfEssence = card("Cauldron of Essence") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Artifact"
    oracleText = "Whenever a creature you control dies, each opponent loses 1 life and you gain 1 life.\n" +
        "{1}{B}{G}, {T}, Sacrifice a creature: Return target creature card from your graveyard " +
        "to the battlefield. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        effect = Effects.Composite(
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(1)
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}{G}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature)
        )
        timing = TimingRule.SorcerySpeed
        val t = target("target", Targets.CreatureCardInYourGraveyard)
        effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Craig J Spearing"
        flavorText = "In the gloomy depths of Widdershins Hall sits an iron belly—always " +
            "hungry, but generous to those who feed it."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7091740-e70c-4cf2-8d3d-b8e1ac1fbbdd.jpg?1775938230"
    }
}
