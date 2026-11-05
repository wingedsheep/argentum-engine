package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dreamstealer — Hour of Devastation #63 (canonical printing)
 * {2}{B} · Creature — Human Wizard · 1/2
 *
 * Menace
 * Whenever this creature deals combat damage to a player, that player discards that many cards.
 * Eternalize {4}{B}{B}
 *
 * "That many" is the amount on the triggering damage event (`triggerDamageAmount()`, as on Westgate
 * Regent), not the creature's power — so pumps, damage doubling and the eternalized 4/4 token all
 * count the damage actually dealt. "That player" is the damaged player (`Player.TriggeringPlayer`).
 */
val Dreamstealer = card("Dreamstealer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "Menace\n" +
        "Whenever this creature deals combat damage to a player, that player discards that many cards.\n" +
        "Eternalize {4}{B}{B} ({4}{B}{B}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Human Wizard with no mana cost. Eternalize only as a sorcery.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Discard(
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
    }

    eternalize("{4}{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c4fcde3-03c9-43e6-a34b-29f6ce44e7c4.jpg?1783936042"
        ruling("2017-07-14", "For each card with eternalize, a corresponding game play supplement token can be found in some Hour of Devastation booster packs. These supplements are not required to play with cards with eternalize; you can use the same items to represent an eternalized token as you would any other token.")
        ruling("2017-07-14", "If a creature card with eternalize is put into your graveyard during your main phase, you'll have priority immediately afterward. You can activate its eternalize ability before any player can try to exile it, such as with Crook of Condemnation, if it's legal for you to do so.")
        ruling("2017-07-14", "Once you've activated an eternalize ability, the card is immediately exiled. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
        ruling("2017-07-14", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created. Similarly, any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities that the token has copied will also work.")
    }
}
