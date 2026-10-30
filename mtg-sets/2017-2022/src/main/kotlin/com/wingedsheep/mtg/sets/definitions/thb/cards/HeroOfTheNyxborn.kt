package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hero of the Nyxborn
 * {1}{R}{W}
 * Enchantment Creature — Human Soldier
 * 2/2
 * When this creature enters, create a 1/1 white Human Soldier creature token.
 * Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn.
 *
 * An enters token (Elspeth, Sun's Nemesis's Human Soldier) plus the Hero of the Pride trigger.
 */
val HeroOfTheNyxborn = card("Hero of the Nyxborn") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a 1/1 white Human Soldier creature token.\nWhenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455",
        )
        description = "When this creature enters, create a 1/1 white Human Soldier creature token."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = "Whenever you cast a spell that targets this creature, creatures you control get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Victor Adame Minguez"
        flavorText = "Heroic deeds inspire new heroes."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9854e673-1816-4a76-b9db-bb399ac7489f.jpg?1783931522"
        ruling("2020-01-24", "An ability that triggers when you cast a spell resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2020-01-24", "If you cast a spell that targets this creature multiple times, this ability triggers just once.")
        ruling("2020-01-24", "This ability doesn't trigger if you copy a spell that targets it, or if a spell's targets are changed to target it.")
        ruling("2020-01-24", "This effect affects only creatures you control at the time the ability resolves. Creatures you begin to control later in the turn won't get +1/+0.")
    }
}
