package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hour of Eternity — Hour of Devastation #36 (canonical printing)
 * {X}{X}{U}{U}{U} · Sorcery
 *
 * Exile X target creature cards from your graveyard. For each card exiled this way, create a token
 * that's a copy of that card, except it's a 4/4 black Zombie.
 *
 * Exactly X targets (not "up to"). [moveTracked] records only the cards that actually reached exile,
 * so a target that left the graveyard before resolution yields no token. Each token copies the
 * exiled card's printed characteristics (Red Sun's Twilight's destroy-then-copy shape). "A 4/4 black
 * Zombie" states a color and creature type without "in addition to", so both replace the copied ones
 * (CR 205.1a) — `overrideColors` / `overrideSubtypes`, as on The Scarab God — while card types and
 * supertypes are kept. Unlike eternalize, the token keeps the copied card's mana cost (ruling).
 */
val HourOfEternity = card("Hour of Eternity") {
    manaCost = "{X}{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exile X target creature cards from your graveyard. For each card exiled this way, " +
        "create a token that's a copy of that card, except it's a 4/4 black Zombie."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD),
            exactly = DynamicAmounts.xValue(),
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(chosen, CardDestination.ToZone(Zone.EXILE))
            run(
                Effects.ForEachInCollection(
                    collection = exiled,
                    effect = Effects.CreateTokenCopyOfTarget(
                        target = EffectTarget.IterationEntity,
                        overridePower = 4,
                        overrideToughness = 4,
                        overrideColors = setOf(Color.BLACK),
                        overrideSubtypes = setOf(Subtype("Zombie")),
                    ),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Tyler Jacobson"
        flavorText = "To serve as an Eternal is the afterlife granted to the worthy."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01040ed3-4f64-4e47-8f80-3d3a339004f7.jpg?1783936052"
        ruling("2017-07-14", "Each token copies exactly what was printed on the original card and nothing else, except the characteristics it specifically modifies. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2017-07-14", "The tokens are Zombies instead of their other types (unlike Zombies created by an eternalize ability) and are black instead of their other colors. Their power and toughness are 4/4. These are copiable values of the tokens that other effects may copy.")
        ruling("2017-07-14", "Unlike the tokens created by an eternalize ability, the tokens have the mana cost and thus mana value of the cards they're copying.")
        ruling("2017-07-14", "If a card copied by one of the tokens had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created. Similarly, any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities that the token has copied will also work.")
    }
}
