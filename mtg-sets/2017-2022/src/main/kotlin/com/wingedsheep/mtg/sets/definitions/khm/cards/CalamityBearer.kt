package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Calamity Bearer — Kaldheim #125 (canonical printing)
 * {2}{R}{R} · Creature — Giant Berserker 3/4
 *
 * A [DoubleDamage] replacement (CR 616) scoped to Giant sources you control. "A permanent or player"
 * is the unscoped default recipient, and the unrestricted "deal damage" covers combat and noncombat
 * alike. The doubled damage stays attributed to the original source, as the rulings require.
 */
val CalamityBearer = card("Calamity Bearer") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Berserker"
    power = 3
    toughness = 4
    oracleText = "If a Giant source you control would deal damage to a permanent or player, " +
        "it deals double that damage to that permanent or player instead."

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withSubtype("Giant").youControl(),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Simon Dominic"
        flavorText = "He'd ignite the World Tree itself if he could."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a15edad1-ff68-4f20-95f6-99fe549bea98.jpg?1783928234"
        ruling("2021-02-05", "The damage is dealt by the same source as the original source of damage. The doubled damage isn't dealt by Calamity Bearer unless it was the original source of damage.")
        ruling("2021-02-05", "If another effect (or effects) modifies how much damage your Giant source would deal—by preventing some of it, for example—the player being dealt damage or the controller of the permanent being dealt damage chooses the order in which any such effects (including Calamity Bearer's) apply. If all of the damage is prevented, Calamity Bearer's effect no longer applies.")
        ruling("2021-02-05", "If damage dealt by a Giant source you control is being divided or assigned among multiple permanents and/or players, that damage is divided or assigned before doubling. For example, if you attack with a 5/5 Giant with trample and it's blocked by a 2/2 creature, you can assign 2 damage to the blocker and 3 damage to the defending player. Those amounts are then doubled to 4 and 6, respectively.")
    }
}
