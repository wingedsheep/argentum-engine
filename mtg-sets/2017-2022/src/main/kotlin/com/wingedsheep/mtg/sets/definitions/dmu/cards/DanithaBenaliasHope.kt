package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Danitha, Benalia's Hope — Dominaria United #15
 * {4}{W} · Legendary Creature — Human Knight · 4/4
 *
 * First strike, vigilance, lifelink
 * When Danitha enters, you may put an Aura or Equipment card from your hand or graveyard onto the
 * battlefield attached to Danitha.
 *
 * The pick is a `chooseUpTo(1)` over hand + graveyard — choosing nothing *is* the "you may". The
 * move is `MoveCollectionEffect.attachTo = Self`, which splits along the two rulings:
 *  - An **Aura** enters attached to Danitha with no enchant choice, or — when Danitha has left the
 *    battlefield or can't legally be enchanted by it — stays in its previous zone (CR 303.4i).
 *  - An **Equipment** enters attached to Danitha, or — when Danitha has left the battlefield or
 *    can't legally be equipped by it — enters unattached (CR 301.5e).
 */
val DanithaBenaliasHope = card("Danitha, Benalia's Hope") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Knight"
    power = 4
    toughness = 4
    oracleText = "First strike, vigilance, lifelink\n" +
        "When Danitha enters, you may put an Aura or Equipment card from your hand or graveyard onto " +
        "the battlefield attached to Danitha."

    keywords(Keyword.FIRST_STRIKE, Keyword.VIGILANCE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val candidates = gather(
                CardSource.FromMultipleZones(
                    listOf(Zone.HAND, Zone.GRAVEYARD),
                    Player.You,
                    GameObjectFilter.Any.withAnySubtype("Aura", "Equipment")
                )
            )
            val putting = chooseUpTo(
                1,
                from = candidates,
                prompt = "Put an Aura or Equipment card onto the battlefield attached to Danitha"
            )
            move(putting, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You), attachTo = EffectTarget.Self)
        }
        description = "When Danitha enters, you may put an Aura or Equipment card from your hand or " +
            "graveyard onto the battlefield attached to Danitha."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/845e5663-2959-44ae-b4c6-d6ea41f01d6e.jpg?1783921367"
        ruling(
            "2022-09-09",
            "If you attempt to put an Aura onto the battlefield this way and Danitha is no longer on the " +
                "battlefield or that Aura can't legally be attached to Danitha, that Aura does not enter the " +
                "battlefield at all. Instead, it will stay in its previous zone."
        )
        ruling(
            "2022-09-09",
            "Conversely, if you attempt to put a non-Aura Equipment onto the battlefield this way and Danitha " +
                "is no longer on the battlefield or that Equipment can't legally be attached to Danitha, that " +
                "Equipment will enter the battlefield unattached."
        )
    }
}
