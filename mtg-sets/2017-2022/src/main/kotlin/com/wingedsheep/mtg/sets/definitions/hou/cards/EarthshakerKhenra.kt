package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.eternalize
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Earthshaker Khenra — Hour of Devastation #90 (canonical printing)
 * {1}{R} · Creature — Jackal Warrior · 2/1
 *
 * Haste
 * When this creature enters, target creature with power less than or equal to this creature's
 * power can't block this turn.
 * Eternalize {4}{R}{R}
 *
 * The power cap is `powerAtMostEntity(EffectTarget.Self)` (Old Man of the Sea's predicate): the
 * candidate's projected power is compared with this permanent's projected power at target choice
 * and again at resolution, so the eternalized 4/4 token reaches creatures with power up to 4.
 */
val EarthshakerKhenra = card("Earthshaker Khenra") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal Warrior"
    power = 2
    toughness = 1
    oracleText = "Haste\n" +
        "When this creature enters, target creature with power less than or equal to this creature's power can't block this turn.\n" +
        "Eternalize {4}{R}{R} ({4}{R}{R}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a 4/4 black Zombie Jackal Warrior with no mana cost. Eternalize only as a sorcery.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.powerAtMostEntity(EffectTarget.Self)))
        effect = Effects.CantBlock(creature)
    }

    eternalize("{4}{R}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d60eabe-b281-4eba-9c26-287364ed2067.jpg?1783936030"
        ruling("2017-07-14", "The target creature's power is checked when you target it with Earthshaker Khenra's ability and when that ability resolves. Once the ability resolves, if the creature's power increases or Earthshaker Khenra's power decreases, the target creature will still be unable to block.")
        ruling("2017-07-14", "Spells, activated abilities, and triggered abilities can't raise Earthshaker Khenra's power before you have to choose a target for its ability. Static abilities, such as that of Lord of the Accursed, will raise its power in time to let you target a larger creature.")
        ruling("2017-07-14", "For each card with eternalize, a corresponding game play supplement token can be found in some Hour of Devastation booster packs. These supplements are not required to play with cards with eternalize; you can use the same items to represent an eternalized token as you would any other token.")
        ruling("2017-07-14", "If a creature card with eternalize is put into your graveyard during your main phase, you'll have priority immediately afterward. You can activate its eternalize ability before any player can try to exile it, such as with Crook of Condemnation, if it's legal for you to do so.")
        ruling("2017-07-14", "Once you've activated an eternalize ability, the card is immediately exiled. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-07-14", "The token copies exactly what was printed on the original card and nothing else, except the characteristics specifically modified by eternalize. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The token is a Zombie in addition to its other types and is black instead of its other colors. Its base power and toughness are 4/4. It has no mana cost, and thus its mana value is 0. These are copiable values of the token that other effects may copy.")
        ruling("2017-07-14", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created. Similarly, any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities that the token has copied will also work.")
    }
}
