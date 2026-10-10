package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Comet Crawler
 * {2}{B}
 * Creature — Insect Horror
 * Lifelink
 * Whenever this creature attacks, you may sacrifice another creature or artifact. If you do, this creature gets +2/+0 until end of turn.
 */
val CometCrawler = card("Comet Crawler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Horror"
    power = 2
    toughness = 3
    oracleText = "Lifelink\nWhenever this creature attacks, you may sacrifice another creature or artifact. If you do, this creature gets +2/+0 until end of turn."

    keywords(Keyword.LIFELINK)

    // Triggered ability: When attacks, may sacrifice another creature or artifact for +2/+0
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature.or(GameObjectFilter.Artifact), excludeSource = true),
            then = Effects.ModifyStats(2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Cristi Balanescu"
        flavorText = "\"Found the stranded crew in an icy labyrinth. Well, what's left of them.\"\n—Decrypted log 2.23"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/becca990-ab2e-4aa4-be7d-293ec727cb08.jpg?1753683182"
    }
}
