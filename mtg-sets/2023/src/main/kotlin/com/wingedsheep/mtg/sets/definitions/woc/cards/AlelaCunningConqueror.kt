package com.wingedsheep.mtg.sets.definitions.woc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alela, Cunning Conqueror
 * {2}{U}{B}
 * Legendary Creature — Faerie Warlock
 * 2/4
 * Flying
 * Whenever you cast your first spell during each opponent's turn, create a 1/1 black Faerie
 * Rogue creature token with flying.
 * Whenever one or more Faeries you control deal combat damage to a player, goad target creature
 * that player controls.
 *
 * The first trigger is [WavebreakHippocamp]'s shape: `castsNth(1)` gated on an opponent's turn.
 * The second is the per-player batch trigger (fires once for each player hit, binding that player
 * as `Player.TriggeringPlayer`), with the goad target scoped to creatures that player controls —
 * the same targeting Grenzo, Havoc Raiser uses.
 */
val AlelaCunningConqueror = card("Alela, Cunning Conqueror") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Faerie Warlock"
    oracleText = "Flying\n" +
        "Whenever you cast your first spell during each opponent's turn, create a 1/1 black Faerie Rogue " +
        "creature token with flying.\n" +
        "Whenever one or more Faeries you control deal combat damage to a player, goad target creature " +
        "that player controls."
    power = 2
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        triggerRestriction = Conditions.IsOpponentsTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Faerie", "Rogue"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d680c3f1-a3c0-4f38-8958-2402612c8dd1.jpg?1783914993",
        )
        description = "Whenever you cast your first spell during each opponent's turn, create a 1/1 black " +
            "Faerie Rogue creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype("Faerie")).dealCombatDamageToAPlayer()
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.targetPlayerControls(
                    EffectTarget.PlayerRef(Player.TriggeringPlayer),
                ),
            ),
        )
        effect = Effects.Goad(creature)
        description = "Whenever one or more Faeries you control deal combat damage to a player, goad target " +
            "creature that player controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79ba5c35-6e5c-406a-b95f-844d5ec296ab.jpg?1783914990"

        ruling(
            "2023-09-01",
            "Normally, combat damage is dealt all at the same time. In that case, Alela, Cunning Conqueror's " +
                "last ability triggers once for each player that Faeries you control dealt combat damage to, " +
                "regardless of how many creatures were dealing that damage. If any of those Faeries have double " +
                "strike or first strike, the ability will trigger once for each player dealt damage in each " +
                "combat damage step."
        )
    }
}
