package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Barrowin of Clan Undurr
 * {2}{W}{B}
 * Legendary Creature — Dwarf Cleric
 * 3/3
 * When Barrowin enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 * Whenever Barrowin attacks, return up to one creature card with mana value 3 or less from your
 * graveyard to the battlefield if you've completed a dungeon.
 *
 * The attack trigger doesn't target, and its "if you've completed a dungeon" is not an
 * intervening-if (it trails the effect rather than following the trigger condition): the ability
 * always triggers and checks the condition only as it resolves, then you choose up to one
 * matching card from your graveyard.
 */
val BarrowinOfClanUndurr = card("Barrowin of Clan Undurr") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Dwarf Cleric"
    power = 3
    toughness = 3
    oracleText = "When Barrowin enters, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "Whenever Barrowin attacks, return up to one creature card with mana value 3 or less from your " +
        "graveyard to the battlefield if you've completed a dungeon."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.If(
            condition = Conditions.YouCompletedADungeon,
            then = Effects.Pipeline {
                val graveyard = gather(
                    CardSource.FromZone(
                        Zone.GRAVEYARD,
                        Player.You,
                        GameObjectFilter.Creature.manaValueAtMost(3)
                    )
                )
                val chosen = chooseUpTo(
                    1,
                    from = graveyard,
                    prompt = "Return up to one creature card with mana value 3 or less to the battlefield"
                )
                move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/6896c311-5593-4437-b945-43ade9665e43.jpg?1783926450"
        ruling("2021-07-23", "Barrowin of Clan Undurr's last ability works even if it wasn't on the battlefield when you completed a dungeon.")
        ruling("2021-07-23", "The player venturing into the dungeon chooses which dungeon they will venture into. They may choose a dungeon that they have already completed this game.")
        ruling("2021-07-23", "If you somehow venture into the dungeon while a room's ability is on the stack, you will continue on in the dungeon. If you're already in the last room, complete that dungeon and start a new one.")
    }
}
