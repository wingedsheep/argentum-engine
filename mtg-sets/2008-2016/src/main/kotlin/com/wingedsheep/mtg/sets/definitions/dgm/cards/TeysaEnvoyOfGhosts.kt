package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teysa, Envoy of Ghosts — Dragon's Maze #108
 * {5}{W}{B} · Legendary Creature — Human Advisor · 4/4
 *
 * Vigilance, protection from creatures
 * Whenever a creature deals combat damage to you, destroy that creature. Create a 1/1 white and
 * black Spirit creature token with flying.
 *
 * - Protection from creatures is the printed card-type protection (Sparkhunter Masticore's
 *   `ProtectionScope.CardType`), enforced at targeting, blocking and damage.
 * - The trigger is Hixus, Prison Warden's `Triggers.a(Creature).dealsCombatDamage(Recipient.You)`:
 *   once per creature, binding the damage source as the triggering entity. The destroy doesn't
 *   target (ruling), and the token is created whether or not the destroy succeeds — a `then`
 *   sequence runs its second step regardless (ruling). Teysa's controller gets the token.
 */
val TeysaEnvoyOfGhosts = card("Teysa, Envoy of Ghosts") {
    manaCost = "{5}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 4
    toughness = 4
    oracleText = "Vigilance, protection from creatures\n" +
        "Whenever a creature deals combat damage to you, destroy that creature. Create a 1/1 white " +
        "and black Spirit creature token with flying."

    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.CardType("Creature")))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.You)
        effect = Effects.Destroy(EffectTarget.TriggeringEntity) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE, Color.BLACK),
                creatureTypes = setOf("Spirit"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/e/f/ef77c8eb-aa24-46d5-8036-6651c7602383.jpg?1783911111",
            )
        description = "Whenever a creature deals combat damage to you, destroy that creature. " +
            "Create a 1/1 white and black Spirit creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbd8183c-6967-4332-b822-02b82c14ef2d.jpg?1783940020"
        ruling(
            "2013-07-01",
            "You get a Spirit creature token even if Teysa's triggered ability doesn't destroy the " +
                "creature (perhaps because it regenerated or has indestructible)."
        )
        ruling(
            "2013-04-15",
            "Protection from creatures means that Teysa, Envoy of Ghosts can't be the target of abilities " +
                "of creatures or abilities of creature cards in other zones, such as bloodrush abilities. " +
                "Teysa also can't be blocked by creatures, and all damage that would be dealt to Teysa by " +
                "creatures is prevented."
        )
        ruling(
            "2013-04-15",
            "Teysa's last ability doesn't target the creature. It will destroy a creature with hexproof " +
                "or protection from white, for example."
        )
        ruling(
            "2013-04-15",
            "Teysa's controller gets the Spirit creature token, not the controller of the creature that " +
                "dealt combat damage."
        )
    }
}
