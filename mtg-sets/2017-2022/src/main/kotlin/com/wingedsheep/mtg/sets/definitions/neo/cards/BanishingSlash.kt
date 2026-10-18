package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Banishing Slash — Kamigawa: Neon Dynasty #3 (canonical printing)
 * {W}{W} · Sorcery
 *
 * Destroy up to one target artifact, enchantment, or tapped creature. Then if you control an artifact
 * and an enchantment, create a 2/2 white Samurai creature token with vigilance.
 *
 * The control check runs after the destroy resolves, and as two independent existence checks, so a single
 * artifact enchantment satisfies it (2022-02-18 ruling).
 */
val BanishingSlash = card("Banishing Slash") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy up to one target artifact, enchantment, or tapped creature. Then if you control " +
        "an artifact and an enchantment, create a 2/2 white Samurai creature token with vigilance."

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.Artifact or GameObjectFilter.Enchantment or GameObjectFilter.Creature.tapped()
            ),
            optional = true,
        )
        effect = Effects.Destroy(t) then Effects.If(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Artifact),
                Conditions.YouControl(GameObjectFilter.Enchantment),
            ),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Samurai"),
                keywords = setOf(Keyword.VIGILANCE),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Josu Hernaiz"
        flavorText = "\"In the name of Kyodai, begone!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4b8946b-6604-49ca-98db-18a540b1b4e5.jpg?1783923927"
        ruling(
            "2022-02-18",
            "If you control an artifact that is also an enchantment as Banishing Slash resolves, you will " +
                "create a Samurai token."
        )
        ruling(
            "2022-02-18",
            "You may cast Banishing Slash without choosing a target. If you do, and you control both an " +
                "artifact and an enchantment as it resolves, you'll just create a Samurai token. However, if you " +
                "do choose a target and that target is an illegal target as Banishing Slash tries to resolve, " +
                "Banishing Slash won't resolve and none of its effects will happen. The illegal target won't be " +
                "destroyed and you won't create a Samurai token."
        )
    }
}
