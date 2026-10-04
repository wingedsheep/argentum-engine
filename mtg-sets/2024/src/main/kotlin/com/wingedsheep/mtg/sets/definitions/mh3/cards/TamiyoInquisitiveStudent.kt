package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tamiyo, Inquisitive Student // Tamiyo, Seasoned Scholar — Modern Horizons 3 #242
 * {U} · Legendary Creature — Moonfolk Wizard 0/3 // Legendary Planeswalker — Tamiyo (loyalty 2)
 *
 * Modeling notes:
 *  - Front: "When you draw your third card in a turn" is [Triggers.you] `drawsNth(3)`; the
 *    transform is [Effects.ExileAndReturnTransformed] — a new object enters back face up with its
 *    printed loyalty 2.
 *  - +2 is Jace, Reality Sculptor's shape: Tamiyo's own delayed trigger lasting until your next
 *    turn ([DelayedTriggerExpiry.UntilControllersNextTurn]), firing once per creature declared
 *    attacking you or a planeswalker you control, and giving that attacker (the triggering
 *    entity) -1/-0 until end of turn. It keeps working if Tamiyo leaves.
 *  - −3 checks the card's colour while it is still the graveyard card (colour is invariant across
 *    the move), the Cemetery Recruitment idiom.
 *  - −7: half the library is `count(LIBRARY) divRoundedUp 2`, read on resolution. The emblem's
 *    "You have no maximum hand size" is the rest-of-game, timestamped player property
 *    ([Effects.RemoveMaximumHandSize]) — the same CR 613.11 timestamp ordering the Necrodominance
 *    ruling describes — plus a [Effects.CreatePermanentEmblem] badge naming it.
 */
private val TamiyoInquisitiveStudentFront = card("Tamiyo, Inquisitive Student") {
    manaCost = "{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Moonfolk Wizard"
    power = 0
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever Tamiyo attacks, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "When you draw your third card in a turn, exile Tamiyo, then return her to the " +
        "battlefield transformed under her owner's control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.Investigate()
    }

    triggeredAbility {
        trigger = Triggers.you.drawsNth(3)
        effect = Effects.ExileAndReturnTransformed(EffectTarget.Self)
        description = "When you draw your third card in a turn, exile Tamiyo, then return her to " +
            "the battlefield transformed under her owner's control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a717b98-cdac-416d-bf6c-f6b6638e65d1.jpg?1783911236"

        ruling("2024-06-07", "In some rare cases, a spell or ability may cause Tamiyo, Inquisitive Student to transform while she's a creature (front face up) on the battlefield. If this happens, Tamiyo, Seasoned Scholar won't have any loyalty counters on her and will subsequently be put into her owner's graveyard.")
    }
}

private val TamiyoSeasonedScholar = card("Tamiyo, Seasoned Scholar") {
    manaCost = ""
    colorIdentity = "UG"
    colorIndicator = "UG"
    typeLine = "Legendary Planeswalker — Tamiyo"
    startingLoyalty = 2
    oracleText = "+2: Until your next turn, whenever a creature attacks you or a planeswalker you " +
        "control, it gets -1/-0 until end of turn.\n" +
        "−3: Return target instant or sorcery card from your graveyard to your hand. If it's a " +
        "green card, add one mana of any color.\n" +
        "−7: Draw cards equal to half the number of cards in your library, rounded up. You get an " +
        "emblem with \"You have no maximum hand size.\""

    loyaltyAbility(+2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks(),
            effect = Effects.ModifyStats(-1, 0, EffectTarget.TriggeringEntity),
            expiry = DelayedTriggerExpiry.UntilControllersNextTurn,
        )
        description = "Until your next turn, whenever a creature attacks you or a planeswalker you " +
            "control, it gets -1/-0 until end of turn."
    }

    loyaltyAbility(-3) {
        val spell = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.GREEN), spell),
            then = Effects.Move(spell, Zone.HAND) then Effects.AddAnyColorMana(1),
            otherwise = Effects.Move(spell, Zone.HAND),
        )
        description = "Return target instant or sorcery card from your graveyard to your hand. If " +
            "it's a green card, add one mana of any color."
    }

    loyaltyAbility(-7) {
        effect = Effects.DrawCards(DynamicAmounts.count(Player.You, Zone.LIBRARY) divRoundedUp 2) then
            Effects.RemoveMaximumHandSize() then
            Effects.CreatePermanentEmblem(emblemDescription = "You have no maximum hand size.")
        description = "Draw cards equal to half the number of cards in your library, rounded up. " +
            "You get an emblem with \"You have no maximum hand size.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "242"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/back/2/a/2a717b98-cdac-416d-bf6c-f6b6638e65d1.jpg?1783911236"

        ruling("2024-06-07", "You can activate one of Tamiyo, Seasoned Scholar's loyalty abilities the turn she enters the battlefield. However, you may do so only during one of your main phases when the stack is empty. For example, if Tamiyo, Seasoned Scholar enters the battlefield during combat, there will be an opportunity for your opponent to remove her before you can activate one of her abilities.")
        ruling("2024-06-07", "If multiple effects modify your hand size, apply them in timestamp order. For example, if you put Necrodominance (an enchantment that says your maximum hand size is five) onto the battlefield and then activate Tamiyo, Seasoned Scholar's last ability, you'll have no maximum hand size. However, if the emblem from Tamiyo, Seasoned Scholar's last ability was created before you put Necrodominance onto the battlefield, your maximum hand size would be five.")
    }
}

val TamiyoInquisitiveStudent: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TamiyoInquisitiveStudentFront,
    backFace = TamiyoSeasonedScholar,
)
