package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Invocation of Saint Traft
 * {1}{W}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature has "Whenever this creature attacks, create a 4/4 white Angel creature token
 * with flying that's tapped and attacking. Exile that token at end of combat."
 *
 * Geist of Saint Traft's attack trigger, granted to the enchanted creature the Ghostly Touch way
 * ([GrantTriggeredAbility] over the default attached-creature filter). As a granted ability it is
 * controlled by the creature's controller, so the Angel is theirs even when the Aura's controller
 * is someone else. The end-of-combat exile is a delayed trigger, so it still fires if the Aura or
 * the creature leaves the battlefield first.
 */
val InvocationOfSaintTraft = card("Invocation of Saint Traft") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"Whenever this creature attacks, create a 4/4 white Angel creature " +
        "token with flying that's tapped and attacking. Exile that token at end of combat.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantTriggeredAbility(
            grantedTriggeredAbility {
                trigger = Triggers.self.attacks()
                effect = Effects.CreateToken(
                    power = 4,
                    toughness = 4,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Angel"),
                    keywords = setOf(Keyword.FLYING),
                    tapped = true,
                    attacking = true,
                    imageUri = "https://cards.scryfall.io/normal/front/b/d/bdb975fe-ac30-4249-bb55-7efb64645e4d.jpg?1783937685",
                ) then Effects.CreateDelayedTrigger(
                    step = Step.END_COMBAT,
                    effect = Effects.Move(
                        target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                        destination = Zone.EXILE,
                    ),
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c64077da-9e55-4ced-91f7-db478383172f.jpg?1783937714"
        ruling(
            "2016-04-08",
            "You declare which player or planeswalker the token is attacking as you put it onto the " +
                "battlefield. It doesn't have to be the same player or planeswalker the enchanted creature is attacking."
        )
        ruling(
            "2016-04-08",
            "Although the token is attacking, it was never declared as an attacking creature (for purposes of " +
                "abilities that trigger whenever a creature attacks, for example)."
        )
        ruling(
            "2016-04-08",
            "Removing Invocation of Saint Traft or the enchanted creature from the battlefield won't stop the " +
                "delayed triggered ability from exiling the Angel token at end of combat."
        )
    }
}
