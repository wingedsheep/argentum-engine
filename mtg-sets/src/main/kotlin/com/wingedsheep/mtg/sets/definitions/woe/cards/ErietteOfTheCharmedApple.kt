package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eriette of the Charmed Apple
 * {1}{W}{B}
 * Legendary Creature — Human Warlock
 * 2/4
 *
 * Each creature that's enchanted by an Aura you control can't attack you or planeswalkers you
 * control.
 * At the beginning of your end step, each opponent loses X life and you gain X life, where X is
 * the number of Auras you control.
 */
val ErietteOfTheCharmedApple = card("Eriette of the Charmed Apple") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Warlock"
    oracleText = "Each creature that's enchanted by an Aura you control can't attack you or " +
        "planeswalkers you control.\nAt the beginning of your end step, each opponent loses X life " +
        "and you gain X life, where X is the number of Auras you control."
    power = 2
    toughness = 4

    staticAbility {
        ability = CantAttack(
            GroupFilter(GameObjectFilter.Creature.enchantedByAura())
        )
    }

    val aurasYouControl = DynamicAmounts
        .battlefield(
            Player.You,
            GameObjectFilter.Enchantment.withSubtype(Subtype.AURA),
        )
        .count()

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.LoseLife(
            aurasYouControl,
            EffectTarget.PlayerRef(Player.EachOpponent),
        ) then Effects.GainLife(aurasYouControl)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "202"
        artist = "Magali Villeneuve"
        flavorText = "\"Hush now. I need your beauty sleep.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecead4cd-47ae-4c42-b15c-1b29b5caba18.jpg?1783915073"

        ruling(
            "2023-09-01",
            "By default, the controller of a Role token is the player who created it, even if that " +
                "Role token is attached to a creature they don't control."
        )
        ruling(
            "2023-09-01",
            "Count the number of Auras you control as the ability resolves to determine the value " +
                "of X."
        )
    }
}
