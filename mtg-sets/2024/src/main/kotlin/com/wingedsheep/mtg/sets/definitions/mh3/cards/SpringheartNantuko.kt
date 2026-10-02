package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Springheart Nantuko (Modern Horizons 3 #171)
 *
 * The landfall payoff reads "if you didn't create a token this way", not "if you didn't pay":
 * every path that ends without a copy token — not attached to a creature you control, declining
 * the {1}{G}, or paying but the copy making nothing — falls through to the 1/1 Insect. The copy
 * branch is scored on `CREATED_TOKENS` so a paid-for copy that produced no token still yields the
 * Insect. "That creature" is the creature this permanent is attached to as the ability resolves.
 */
val SpringheartNantuko = card("Springheart Nantuko") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Insect Monk"
    power = 1
    toughness = 1
    oracleText = "Bestow {1}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Landfall — Whenever a land you control enters, you may pay {1}{G} if this permanent is attached to a creature you control. If you do, create a token that's a copy of that creature. If you didn't create a token this way, create a 1/1 green Insect creature token."

    keywordAbility(KeywordAbility.bestow("{1}{G}"))

    staticAbility { ability = ModifyStats(1, 1) }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val insect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Insect"),
            imageUri = "https://cards.scryfall.io/normal/front/0/5/059e9d51-c7bf-41c0-a6f0-c0bbc6d3b434.jpg?1783911112"
        )
        effect = Effects.If(
            // Read off the source's own attachment: unattached, it has no host (the attachment-host
            // conditions fall back to the source itself, which would see a creature you control).
            condition = Conditions.SourceMatches(
                GameObjectFilter.Any.attachedTo(GameObjectFilter.Creature.youControl())
            ),
            then = Effects.MayPay(
                cost = ManaCost.parse("{1}{G}"),
                then = Effects.IfYouDo(
                    action = Effects.CreateTokenCopyOfTarget(EffectTarget.EnchantedCreature),
                    then = Effects.Nothing,
                    otherwise = insect,
                    successCriterion = SuccessCriterion.CollectionNonEmpty(CREATED_TOKENS)
                ),
                otherwise = insect
            ),
            otherwise = insect
        )
    }

    metadata {
        ruling("2024-06-07", "If you don't pay {1}{G}, either because you simply chose not to or because Springheart Nantuko isn't attached to a creature you control, you'll still create a 1/1 green Insect creature token when Springheart Nantuko's landfall ability resolves.")
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54a3ea87-005e-4985-b2a5-21711d0b71c0.jpg?1783911255"
    }
}
