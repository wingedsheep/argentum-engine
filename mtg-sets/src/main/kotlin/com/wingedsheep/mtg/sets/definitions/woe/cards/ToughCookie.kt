package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Tough Cookie
 * {1}{G}
 * Artifact Creature — Food Golem
 * 2/2
 * When this creature enters, create a Food token.
 * {2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature.
 * {2}, {T}, Sacrifice this creature: You gain 3 life.
 *
 * The animate clause is a plain [Effects.BecomeCreature] with a fixed 4/4 and no `removeTypes`, so
 * the target keeps every type and subtype it already had (2023-09-01 ruling) and keeps its
 * abilities — a Food token animated this way is still a Food that can be sacrificed for life.
 * Tough Cookie itself is a Food, so its own last ability is the Food sacrifice outlet.
 */
val ToughCookie = card("Tough Cookie") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Food Golem"
    oracleText = "When this creature enters, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "{2}{G}: Until end of turn, target noncreature artifact you control becomes a 4/4 artifact creature.\n" +
        "{2}, {T}, Sacrifice this creature: You gain 3 life."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        val t = target(
            "target noncreature artifact you control",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact.notCreature().youControl()))
        )
        effect = Effects.BecomeCreature(
            target = t,
            power = 4,
            toughness = 4,
            addTypes = setOf("ARTIFACT")
        )
        description = "{2}{G}: Until end of turn, target noncreature artifact you control becomes " +
            "a 4/4 artifact creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "193"
        artist = "Milivoj Ćeran"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7aa37f85-5336-4372-97a2-9c8b00798c7a.jpg?1783915075"
        ruling(
            "2023-09-01",
            "As Tough Cookie's second ability resolves, the target permanent keeps any other types " +
                "and subtypes it had before it became an artifact creature."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a creature type."
        )
    }
}
