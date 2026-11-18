package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eidolon of the Great Revel
 * {R}{R}
 * Enchantment Creature — Spirit
 * 2/2
 * Whenever a player casts a spell with mana value 3 or less, this creature deals 2 damage to that player.
 *
 * The same shape as Pyrostatic Pillar: any player's cast (its controller included) triggers, and
 * the damage goes to the caster ([Player.TriggeringPlayer]), not a target.
 */
val EidolonOfTheGreatRevel = card("Eidolon of the Great Revel") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Whenever a player casts a spell with mana value 3 or less, this creature deals 2 damage to that player."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Any.manaValueAtMost(3))
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Cyril Van Der Haegen"
        flavorText = "Xenagos reveled while Theros burned."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6c10816-e825-452a-90b0-80eb9f20bd6d.jpg?1783939426"
        ruling(
            "2018-03-16",
            "Casting Eidolon of the Great Revel doesn't cause its own ability to trigger. It must be on " +
                "the battlefield when the spell is cast for its ability to trigger."
        )
        ruling(
            "2018-03-16",
            "The mana value of a spell with {X} in its mana cost includes the value chosen for X."
        )
    }
}
