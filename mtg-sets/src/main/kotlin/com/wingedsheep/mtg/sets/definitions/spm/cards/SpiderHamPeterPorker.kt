package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spider-Ham, Peter Porker
 * {1}{G}
 * Legendary Creature — Spider Boar Hero
 * 2/2
 *
 * When Spider-Ham enters, create a Food token. (It's an artifact with
 * "{2}, {T}, Sacrifice this token: You gain 3 life.")
 *
 * Animal May-Ham — Other Spiders, Boars, Bats, Bears, Birds, Cats, Dogs, Frogs,
 * Jackals, Lizards, Mice, Otters, Rabbits, Raccoons, Rats, Squirrels, Turtles, and
 * Wolves you control get +1/+1.
 *
 * "Animal May-Ham" is a flavor ability word; the anthem is a static [ModifyStats] over
 * a [GroupFilter] matching any of the listed creature subtypes among creatures you
 * control, excluding Spider-Ham itself ("Other …"). Note the oracle plural "Mice" maps to
 * the singular subtype "Mouse".
 */
val SpiderHamPeterPorker = card("Spider-Ham, Peter Porker") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spider Boar Hero"
    power = 2
    toughness = 2
    oracleText = "When Spider-Ham enters, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Animal May-Ham — Other Spiders, Boars, Bats, Bears, Birds, Cats, Dogs, Frogs, " +
        "Jackals, Lizards, Mice, Otters, Rabbits, Raccoons, Rats, Squirrels, Turtles, and " +
        "Wolves you control get +1/+1."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
        description = "When Spider-Ham enters, create a Food token."
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                baseFilter = GameObjectFilter.Creature.youControl().withAnySubtype(
                    "Spider", "Boar", "Bat", "Bear", "Bird", "Cat", "Dog", "Frog", "Jackal",
                    "Lizard", "Mouse", "Otter", "Rabbit", "Raccoon", "Rat", "Squirrel",
                    "Turtle", "Wolf"
                ),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Filipe Pagliuso"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41f18f42-b86b-4a12-9f0d-76b761571195.jpg?1783905324"
    }
}
