package com.pupscan.ticket.companies

import com.pupscan.ticket.ZTicket
import com.pupscan.ticket.cleanName
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.TextScore
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class CompanyService(val repository: CompanyCommandRepository) {
    val logger = LoggerFactory.getLogger(CompanyService::class.java)!!

    fun recreateVue(zTickets: List<ZTicket>) {
        logger.info("Recreate vue")
        repository.deleteAll()
        val companies = zTickets
                .filter { companyFilter(it) }
                .map {
                    Company(UUID.randomUUID().toString(),
                            it.via.source.from.name.cleanName(),
                            it.via.source.from.address!!)
                }
                .distinctBy { it.email }
        val companiesWithCountry = findAndSetCountryToCompanies(countries, companies, zTickets)
        repository.save(companiesWithCountry)
    }


    private fun findAndSetCountryToCompanies(countries: List<String>,
                                             companies: List<Company>,
                                             zTickets: List<ZTicket>): List<Company> {
        return companies.map { company ->
            val message = zTickets.filter { it.via.source.from.address == company.email }
                    .map { it.description }.reduce { acc, s -> acc + s }
            countries.find { message.contains(it, true) }?.let { company.apply { country = it.replace(" ", "-") } } ?: company
        }
    }


    private fun companyFilter(zTicket: ZTicket): Boolean {
        val status = zTicket.tags
        val mail = zTicket.via.source.from.address ?: ""
        return when {
            mail.isBlank() -> false
            mail.contains("@").not() -> false
            status.contains("distributor") -> true
            status.contains("resseler") -> true
            status.contains("reseller") -> true
            status.contains("Distributeur") -> true
            status.contains("Revendeur") -> true
            else -> false
        }
    }
}


@Document
data class Company(val id: String,
                   @TextIndexed(weight = 5F) val name: String,
                   @TextIndexed(weight = 5F) val email: String,
                   @TextIndexed(weight = 2F) var country: String = "",
                   @TextScore val score: Float? = 0F)

interface CompanyCommandRepository : CrudRepository<Company, String>

val countries = """Afghanistan,Albania,Algeria,American Samoa,Andorra,Angola,Anguilla,Antarctica,Antigua And Barbuda,Argentina,Armenia,Aruba,Australia,
Austria,Azerbaijan,Bahamas,Bahrain,Bangladesh,Barbados,Belarus,Belgium,Belize,Benin,Bermuda,Bhutan,Bolivia,Bosnia And Herzegovina,
Botswana,Bouvet Island,Brazil,British Indian Ocean Territory,Brunei Darussalam,Bulgaria,Burkina Faso,Burundi,Cambodia,Cameroon,Canada,Cape
Verde,Cayman Islands,Central African Republic,Chad,Chile,China,Christmas Island,Cocos (keeling) Islands,Colombia,Comoros,Congo,Congo,The
Democratic Republic Of The,Cook Islands,Costa Rica,Cote D'ivoire,Croatia,Cuba,Cyprus,Czech Republic,Denmark,Djibouti,Dominica,Dominican
Republic,East Timor,Ecuador,Egypt,El Salvador,Equatorial Guinea,Eritrea,Estonia,Ethiopia,Falkland Islands (malvinas),Faroe Islands,Fiji,
Finland,France,French Guiana,French Polynesia,French Southern Territories,Gabon,Gambia,Georgia,Germany,Ghana,Gibraltar,Greece,Greenland,
Grenada,Guadeloupe,Guam,Guatemala,Guinea,Guinea-bissau,Guyana,Haiti,Heard Island And Mcdonald Islands,Holy See (vatican City State),
Honduras,Hong Kong,Hungary,Iceland,India,Indonesia,Iran,Islamic Republic Of,Iraq,Ireland,Israel,Italy,Jamaica,Japan,Jordan,Kazakstan,
Kenya,Kiribati,Korea,Democratic People's Republic Of,Korea,Republic Of,Kosovo,Kuwait,Kyrgyzstan,Lao People's Democratic Republic,Latvia,
Lebanon,Lesotho,Liberia,Libyan Arab Jamahiriya,Liechtenstein,Lithuania,Luxembourg,Macau,Macedonia,The Former Yugoslav Republic Of,
Madagascar,Malawi,Malaysia,Maldives,Mali,Malta,Marshall Islands,Martinique,Mauritania,Mauritius,Mayotte,Mexico,Micronesia,Federated
States Of,Moldova,Republic Of,Monaco,Mongolia,Montserrat,Montenegro,Morocco,Mozambique,Myanmar,Namibia,Nauru,Nepal,Netherlands,
Netherlands Antilles,New Caledonia,New Zealand,Nicaragua,Niger,Nigeria,Niue,Norfolk Island,Northern Mariana Islands,Norway,Oman,Pakistan,
Palau,Palestinian Territory,Occupied,Panama,Papua New Guinea,Paraguay,Peru,Philippines,Pitcairn,Poland,Portugal,Puerto Rico,Qatar,
Reunion,Romania,Russian Federation,Rwanda,Saint Helena,Saint Kitts And Nevis,Saint Lucia,Saint Pierre And Miquelon,Saint Vincent And The
Grenadines,Samoa,San Marino,Sao Tome And Principe,Saudi Arabia,Senegal,Serbia,Seychelles,Sierra Leone,Singapore,Slovakia,Slovenia,Solomon
Islands,Somalia,South Africa,South Georgia And The South Sandwich Islands,Spain,Sri Lanka,Sudan,Suriname,Svalbard And Jan Mayen,Swaziland,
Sweden,Switzerland,Syrian Arab Republic,Taiwan,Province Of China,Tajikistan,Tanzania,United Republic Of,Thailand,Togo,Tokelau,Tonga,
Trinidad And Tobago,Tunisia,Turkey,Turkmenistan,Turks And Caicos Islands,Tuvalu,Uganda,Ukraine,United Arab Emirates,United Kingdom,United
States,United States Minor Outlying Islands,Uruguay,Uzbekistan,Vanuatu,Venezuela,Viet Nam,Virgin Islands,British,Virgin Islands,U.s.,
Wallis And Futuna,Western Sahara,Yemen,Zambia,Zimbabwe""".split(",")