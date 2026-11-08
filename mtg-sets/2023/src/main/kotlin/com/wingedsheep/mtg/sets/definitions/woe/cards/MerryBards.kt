package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Merry Bards
 * {2}{R}
 * Creature — Human Bard
 * 3/2
 *
 * When this creature enters, you may pay {1}. When you do, create a Young Hero Role token attached
 * to target creature you control. (If you control another Role on it, put that one into the
 * graveyard. Enchanted creature has "Whenever this creature attacks, if its toughness is 3 or less,
 * put a +1/+1 counter on it.")
 *
 * Same reflexive "you may pay {1}. When you do" shape as Spellbook Vendor — the target for the Role
 * token isn't chosen when the ETB triggers, but when the reflexive ability goes on the stack after
 * the {1} is paid. The Young Hero Role token carries the granted attack trigger.
 */
val MerryBards = card("Merry Bards") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Bard"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may pay {1}. When you do, create a Young Hero Role " +
        "token attached to target creature you control. (If you control another Role on it, put " +
        "that one into the graveyard. Enchanted creature has \"Whenever this creature attacks, if " +
        "its toughness is 3 or less, put a +1/+1 counter on it.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        // "you may pay {1}. When you do" — the optional payment gates a reflexive trigger, so the
        // target is chosen only after paying (CR 603.12).
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "create a Young Hero Role token " +
                    "attached to target creature you control.",
            ) {
                val targetCreature = target(TargetFilter.CreatureYouControl)
                effect = Effects.CreateRoleToken("Young Hero Role", targetCreature)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0058b9b-e919-45eb-9da0-690f62aa252e.jpg?1783915092"
        ruling("2023-09-01", "You don't choose a target for Merry Bards's ability at the time it triggers. Rather, a second reflexive ability triggers when you pay this way. You choose a target for that ability as it goes on the stack.")
        ruling("2023-09-01", "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and the enchant creature ability.")
    }
}
