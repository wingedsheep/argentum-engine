package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Norin, Swift Survivalist
 * {R}
 * Legendary Creature — Human Coward
 * 2/1
 * Norin can't block.
 * Whenever a creature you control becomes blocked, you may exile it. You may play that card
 * from exile this turn.
 *
 * "Can't block" is the [CantBlock] combat static on the source. The blocked-creature trigger uses
 * the ANY-binding `becomesBlocked` factory filtered to creatures you control (same shape as
 * Gustcloak Savior). The optional effect is a gather → exile → grant pipeline over
 * [CardSource.Entity] ("it" = the just-blocked creature), granting play-from-exile
 * permission only until end of turn ([MayPlayExpiry.EndOfTurn]) — playing it still costs its mana.
 */
val NorinSwiftSurvivalist = card("Norin, Swift Survivalist") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Coward"
    power = 2
    toughness = 1
    oracleText = "Norin can't block.\n" +
        "Whenever a creature you control becomes blocked, you may exile it. You may play that " +
        "card from exile this turn."

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesBlocked()
        effect = Effects.May(
            Effects.Pipeline {
                val exiledBlockedCreature = gather(CardSource.Entity(EffectTarget.TriggeringEntity))
                exile(exiledBlockedCreature)
                run(Effects.GrantMayPlayFromExile(exiledBlockedCreature, MayPlayExpiry.EndOfTurn))
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Yigit Koroglu"
        flavorText = "\"I don't know what it was! Why would I stick around to find out?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49f0fdf4-3881-4327-924f-2c1b67ccda93.jpg?1726286390"
    }
}
