package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Joyful Stormsculptor — {3}{U}{R}
 * Creature — Human Shaman 2/3 (uncommon, MOM #243)
 *
 * When this creature enters, create two 1/1 blue and red Elemental creature tokens.
 * Whenever you cast a spell that has convoke, this creature deals 1 damage to each opponent and
 * each battle they protect.
 *
 * "Each battle they protect" keys on the battle's protector (CR 310.9), not its controller.
 */
val JoyfulStormsculptor = card("Joyful Stormsculptor") {
    manaCost = "{3}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, create two 1/1 blue and red Elemental creature tokens.\n" +
        "Whenever you cast a spell that has convoke, this creature deals 1 damage to each opponent and " +
        "each battle they protect."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/8/28a7a9b0-d823-4b34-829f-ade81fc141e0.jpg?1783916668"
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withKeyword(Keyword.CONVOKE))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Battle.protectedBy()),
                Effects.DealDamage(1, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "243"
        artist = "Christina Kraus"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4dabe69-a88f-4e61-a5fb-c4b9ef1c7569.jpg?1783916942"
        ruling(
            "2023-04-14",
            "Joyful Stormsculptor's last ability will trigger whether you tapped any creatures to pay for " +
                "the spell or not, as long as it has convoke."
        )
    }
}
