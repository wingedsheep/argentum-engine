package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Rem Karolus, Stalwart Slayer (Innistrad: Midnight Hunt #235)
 * {1}{R}{W} · Legendary Creature — Human Knight 2/3
 *
 * Both replacement lines are scoped to *a spell* — any spell, whoever controls it — read as a
 * damage source currently on the stack (Hostility's shape). A creature that resolved and then deals
 * damage, an ability's ping, or a fight a spell causes are not spells dealing damage, so neither
 * line touches them (rulings). "Another permanent you control" excludes Rem itself through
 * `notSourceItself()`. The +1 is a plain additive [ModifyDamageAmount] (Aether Revolt), so the extra
 * point is dealt by the original source.
 */
val RemKarolusStalwartSlayer = card("Rem Karolus, Stalwart Slayer") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Knight"
    power = 2
    toughness = 3
    oracleText = "Flying, haste\n" +
        "If a spell would deal damage to you or another permanent you control, prevent that damage.\n" +
        "If a spell would deal damage to an opponent or a permanent an opponent controls, it deals " +
        "that much damage plus 1 instead."

    keywords(Keyword.FLYING, Keyword.HASTE)

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyOf(
                    listOf(
                        Recipient.You,
                        Recipient.Object(GameObjectFilter.Permanent.youControl().notSourceItself()),
                    )
                ),
                source = GameObjectFilter.Any.currentlyIn(Zone.STACK),
            ),
        )
    )

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.OpponentOrPermanentTheyControl,
                source = GameObjectFilter.Any.currentlyIn(Zone.STACK),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b31e307a-9689-4e11-8c5f-96b7a4b50bed.jpg?1783925556"
        ruling("2021-09-24", "Rem Karolus, Stalwart Slayer's last two abilities apply only to damage dealt by spells on the stack. A spell that becomes a permanent and then deals damage won't be affected.")
        ruling("2021-09-24", "If another effect modifies how much damage a spell would deal to an opponent or a permanent they control, including preventing some of it, the player being dealt damage or the controller of the permanent being dealt damage chooses an order in which to apply those effects. If all of the damage is prevented, Rem's effect no longer applies.")
        ruling("2021-09-24", "The additional 1 damage is dealt by the original source of damage. The additional damage isn't dealt by Rem Karolus.")
        ruling("2021-09-24", "If damage dealt by a source you control is being divided or assigned among multiple permanents an opponent controls or among an opponent and one or more permanents they control, divide the original amount before adding 1.")
        ruling("2021-09-24", "Some spells cause other objects to deal damage, such as spells that cause two creatures to fight. Rem Karolus, Stalwart Slayer's last two abilities don't apply to damage from such effects.")
    }
}
