package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Kairi, the Swirling Sky — Kamigawa: Neon Dynasty #60 (canonical printing)
 * {4}{U}{U} · Legendary Creature — Dragon Spirit · 6/6
 *
 * Flying, ward {3}
 * When Kairi dies, choose one —
 * • Return any number of target nonland permanents with total mana value 6 or less to their owners' hands.
 * • Mill six cards, then return up to two instant and/or sorcery cards from your graveyard to your hand.
 *
 * The cycle's modal dies trigger (Kura, Junji). Mode one is an `unlimited` [TargetObject] with the
 * aggregate [TargetObject.totalManaValueAtMost] cap, held to 6 when the mode's targets are chosen as
 * the trigger goes on the stack. Mode two mills, then picks from the graveyard at resolution — the
 * return doesn't target.
 */
val KairiTheSwirlingSky = card("Kairi, the Swirling Sky") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 6
    toughness = 6
    oracleText = "Flying, ward {3}\n" +
        "When Kairi dies, choose one —\n" +
        "• Return any number of target nonland permanents with total mana value 6 or less to their owners' hands.\n" +
        "• Mill six cards, then return up to two instant and/or sorcery cards from your graveyard to your hand."

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0))),
                TargetObject(
                    filter = TargetFilter.NonlandPermanent,
                    unlimited = true,
                    totalManaValueAtMost = DynamicAmounts.fixed(6),
                ),
                "Return any number of target nonland permanents with total mana value 6 or less to their " +
                    "owners' hands.",
            ),
            Mode.noTarget(
                Patterns.Library.mill(6) then Effects.Pipeline {
                    val spells = gather(
                        CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery),
                    )
                    val returned = chooseUpTo(
                        DynamicAmounts.fixed(2),
                        from = spells,
                        prompt = "Return up to two instant and/or sorcery cards to your hand",
                    )
                    toHand(returned)
                },
                "Mill six cards, then return up to two instant and/or sorcery cards from your graveyard to " +
                    "your hand.",
            ),
        )
        description = "When Kairi dies, choose one — • Return any number of target nonland permanents with " +
            "total mana value 6 or less to their owners' hands. • Mill six cards, then return up to two " +
            "instant and/or sorcery cards from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af7979d8-21f3-40ba-8a2e-7ebad210e48b.jpg?1783923901"
    }
}
