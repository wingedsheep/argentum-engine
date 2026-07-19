package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Guerrilla Gorilla — Marvel Super Heroes #169
 * {1}{G} · Creature — Ape Soldier Hero · Common
 * 2/2
 *
 * Reach
 * Sacrifice this creature: Destroy target noncreature artifact or noncreature enchantment.
 * Activate only as a sorcery.
 *
 * "Noncreature artifact or noncreature enchantment" is the artifact-or-enchantment filter
 * narrowed by [GameObjectFilter.notCreature] — an artifact creature or enchantment creature is
 * not a legal target. `Activate only as a sorcery` is [TimingRule.SorcerySpeed].
 */
val GuerrillaGorilla = card("Guerrilla Gorilla") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ape Soldier Hero"
    power = 2
    toughness = 2
    oracleText = "Reach\n" +
        "Sacrifice this creature: Destroy target noncreature artifact or noncreature " +
        "enchantment. Activate only as a sorcery."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val victim = target(
            "target noncreature artifact or noncreature enchantment",
            TargetPermanent(
                filter = TargetFilter(GameObjectFilter.ArtifactOrEnchantment.notCreature())
            )
        )
        effect = Effects.Destroy(victim)
        timing = TimingRule.SorcerySpeed
        description = "Sacrifice this creature: Destroy target noncreature artifact or " +
            "noncreature enchantment. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Michele Giorgi"
        flavorText = "\"I thought you all were globe-trotting Super Hero types. You act like " +
            "you've never seen a talking gorilla before.\"\n—Gorilla-Man, Ken Hale"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af6bc9d2-5783-489a-8963-af65f54f4f17.jpg?1783902918"
    }
}
